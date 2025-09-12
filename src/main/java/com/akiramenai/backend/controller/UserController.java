package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.service.JWTService;
import com.akiramenai.backend.service.MediaStorageService;
import com.akiramenai.backend.service.UserService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.JsonSerializer;
import com.akiramenai.backend.utility.RefreshTokenHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class UserController {
  private final HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  private final JsonSerializer jsonSerializer = new JsonSerializer();

  private final UserService userService;
  private final MediaStorageService mediaStorageService;
  private final JWTService jwtService;

  @Value("${application.security.jwt.refresh-token-validity-duration}")
  private String refreshTokenValidityDuration;

  @Value("${application.security.jwt.https-only-cookie}")
  private String shouldCookieBeSentUsingHttpsOnly;

  @Value("${application.default-values.media.picture-directory}")
  private String pictureDirectory;

  @Value("${application.default-values.user-profile-picture-filename}")
  private String defaultPictureFilename;

  public UserController(UserService service, MediaStorageService mediaStorageService, JWTService jwtService) {
    this.userService = service;
    this.mediaStorageService = mediaStorageService;
    this.jwtService = jwtService;
  }

  @PostMapping("api/public/register")
  public ResponseEntity<String> register(@RequestBody RegisterRequest registerRequest) {
    Optional<String> failureReason = userService.register(registerRequest);
    if (failureReason.isPresent()) {
      return new ResponseEntity<>(failureReason.get(), HttpStatus.CONFLICT);
    }

    return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
  }

  @PostMapping("api/public/login")
  public void login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
    try {
      AuthenticationResult authResp = userService.authenticate(loginRequest);
      if (authResp.errorMessage() != null) {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().print(authResp.errorMessage());
        response.getWriter().flush();
        return;
      }

      LoginResponse loginResponse = LoginResponse
          .builder()
          .accessToken(authResp.accessToken())
          .accountType(authResp.accountType())
          .build();

      //Cookie refreshTokenCookie = refreshTokenCookieBuilder(authResp.refreshToken());
      Cookie refreshTokenCookie = RefreshTokenHandler.cookieBuilder(
          authResp.refreshToken(),
          Integer.parseInt(refreshTokenValidityDuration),
          Boolean.parseBoolean(shouldCookieBeSentUsingHttpsOnly)
      );

      response.setContentType("application/json");
      response.setStatus(HttpServletResponse.SC_OK);
      ObjectMapper om = new ObjectMapper();
      response.getWriter().print(om.writeValueAsString(loginResponse));
      response.addCookie(refreshTokenCookie);
      response.getWriter().flush();
    } catch (Exception e) {
      log.info(e.getMessage());
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      try {
        response.getWriter().print("Authentication failed. Invalid credentials provided.");
        response.getWriter().flush();
      } catch (Exception e2) {
        log.error("Failed to write HTTP response for `/login` request. Reason: {}", e2.getMessage());
      }
    }
  }

  @PostMapping("api/protected/update-username")
  public ResponseEntity<String> updateUsername(
      HttpServletRequest request,
      @RequestBody UpdateUsernameRequest updateUsernameRequest
  ) {
    if (updateUsernameRequest.getNewUsername() == null) {
      new ResponseEntity<>("No new username provided. Please provide a valid username.", HttpStatus.BAD_REQUEST);
    }
    updateUsernameRequest.setNewUsername(updateUsernameRequest.getNewUsername().trim());
    if (updateUsernameRequest.getNewUsername().isEmpty()) {
      new ResponseEntity<>("New username can't be empty or contain only whitespace characters. Please provide a valid username.", HttpStatus.BAD_REQUEST);
    }

    String currentUserId = request.getAttribute("userId").toString();
    Optional<String> resp = userService.changeUsername(currentUserId, updateUsernameRequest.getNewUsername());
    return resp
        .map(errorReason -> new ResponseEntity<>(errorReason, HttpStatus.INTERNAL_SERVER_ERROR))
        .orElseGet(() -> new ResponseEntity<>("Successfully updated username.", HttpStatus.OK));
  }

  @PostMapping("api/protected/update-password")
  public void updatePassword(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody UpdatePasswordRequest updatePasswordRequest
  ) {
    log.info("Changing password for user {}", updatePasswordRequest.getOldPassword());
    if (updatePasswordRequest.getOldPassword() == null || updatePasswordRequest.getNewPassword() == null) {
      new ResponseEntity<>("Both the old password and the new password needs to provided to update the password.", HttpStatus.BAD_REQUEST);
    }
    updatePasswordRequest.setOldPassword(updatePasswordRequest.getOldPassword().trim());
    updatePasswordRequest.setNewPassword(updatePasswordRequest.getNewPassword().trim());
    if (updatePasswordRequest.getOldPassword().isEmpty() || updatePasswordRequest.getNewPassword().isEmpty()) {
      new ResponseEntity<>("Provided passwords can't be empty or contain only whitespace characters.", HttpStatus.BAD_REQUEST);
    }

    String currentUserId = request.getAttribute("userId").toString();
    Optional<String> resp = userService.changePassword(currentUserId, updatePasswordRequest.getOldPassword(), updatePasswordRequest.getNewPassword());

    if (resp.isPresent()) {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      try {
        response.getWriter().print(resp.get());
        response.getWriter().flush();
      } catch (Exception e) {
        log.error("Failed to write HTTP response for `/update_password` request. Reason: {}", e.getMessage());
      }

      return;
    }

    response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);
    try {
      response.getWriter().print("Successfully updated password.");
      response.getWriter().flush();
    } catch (Exception e) {
      log.error("Failed to write HTTP response for `/update_password` request. Reason: {}", e.getMessage());
    }
  }

  @PostMapping("api/protected/change-profile-picture")
  public ResponseEntity<String> changeProfilePicture(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam("new-profile-picture") MultipartFile newProfilePicture
  ) {
    if (newProfilePicture.isEmpty()) {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      try {
        response.getWriter().print("No profile picture provided.");
        response.getWriter().flush();
      } catch (Exception e) {
        log.error("Failed to send response after profile picture change request. Reason: {}", e.getMessage());
      }
    }

    ResultOrError<String, FileUploadErrorTypes> savedFilePath = mediaStorageService.saveImage(newProfilePicture);
    if (savedFilePath.errorType() != null) {
      switch (savedFilePath.errorType()) {
        case FileIsEmpty -> {
          return new ResponseEntity<>(savedFilePath.errorMessage(), HttpStatus.BAD_REQUEST);
        }
        case InvalidUploadDir, FailedToCreateUploadDir, FailedToSaveFile -> {
          return new ResponseEntity<>(savedFilePath.errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }
    }

    Optional<String> resp = userService.updateProfilePicturePath(request.getAttribute("userId").toString(), savedFilePath.result());
    return resp
        .map(errorReason -> new ResponseEntity<>(errorReason, HttpStatus.INTERNAL_SERVER_ERROR))
        .orElseGet(() -> new ResponseEntity<>("Successfully updated user's profile picture.", HttpStatus.OK));
  }

  @GetMapping("api/protected/get/user-info")
  public void getUserInfo(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    String currentUserId = httpRequest.getAttribute("userId").toString();

    Optional<Users> targetUser = userService.findUserById(UUID.fromString(currentUserId));
    if (targetUser.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed retrieve user info. User not found.", HttpStatus.NOT_FOUND);
      return;
    }

    CleanedUserInfo cleanedUserInfo = new CleanedUserInfo(targetUser.get());

    Optional<String> userInfoJson = jsonSerializer.serialize(cleanedUserInfo);
    if (userInfoJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(httpResponse, userInfoJson.get(), HttpStatus.OK);
  }

  @GetMapping("api/public/get/user-profile-picture/{user-id}")
  public ResponseEntity<InputStreamResource> getUserProfilePicture(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @PathVariable(name = "user-id") String userId
  ) {
    Optional<Users> targetUser = userService.findUserById(UUID.fromString(userId));
    if (targetUser.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Path pictureDirPath = Paths.get(pictureDirectory);
    InputStream is = null;
    try {
      if (targetUser.get().getPfpPath() == null) {
        // return the default profile pic
        is = new FileInputStream(pictureDirPath.resolve(defaultPictureFilename).toFile());
      } else {
        is = new FileInputStream(
            pictureDirPath.resolve(targetUser.get().getPfpPath()).toFile()
        );
      }
    } catch (Exception e) {
      log.error("Failed to get user's profile picture. Reason: {}", e.getMessage());

      return ResponseEntity.internalServerError().build();
    }

    // return their specific profile pic
    return ResponseEntity
        .ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(new InputStreamResource(is));
  }

  @PostMapping("api/public/logout")
  public void logout(HttpServletResponse response) {
    RefreshTokenHandler.removeCookie(response, Boolean.parseBoolean(shouldCookieBeSentUsingHttpsOnly));
    SecurityContextHolder.clearContext();

    response.setStatus(HttpServletResponse.SC_OK);
    try {
      response.getWriter().flush();
    } catch (Exception e) {
      log.error("Failed to write HTTP response for `/logout` request. Reason: {}", e.getMessage());
    }
  }

  @GetMapping("api/public/refresh-access-token")
  public void refreshAccessToken(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    Optional<String> refreshToken = RefreshTokenHandler.getFromCookie(httpRequest);
    if (refreshToken.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "No refresh token provided.", HttpStatus.NOT_FOUND);
      return;
    }

    ResultOrError<Claims, JwtErrorTypes> extractedClaims = jwtService.extractClaim(refreshToken.get());
    if (extractedClaims.errorType() != null) {
      if (extractedClaims.errorType() == JwtErrorTypes.JwtExpiredException) {
        httpResponseWriter.writeFailedResponse(httpResponse, "Refresh token expired.", HttpStatus.BAD_REQUEST);
        return;
      }
      httpResponseWriter.writeFailedResponse(httpResponse, "Invalid refresh token provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    String accessToken = jwtService.generateToken(
        extractedClaims.result().getSubject(),
        extractedClaims.result().get("userId", String.class),
        extractedClaims.result().get("accountType", String.class),
        JWTService.TokenType.AccessToken
    );

    Optional<String> accessTokenJson = jsonSerializer.serialize(new AccessTokenContainer(accessToken));
    if (accessTokenJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(httpResponse, accessTokenJson.get(), HttpStatus.OK);
  }
}
