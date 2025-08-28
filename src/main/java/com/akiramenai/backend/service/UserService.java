package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.InstructorInfosRepo;
import com.akiramenai.backend.repo.LearnerInfosRepo;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.utility.CustomAuthToken;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserService {
  private final Logger logger = log;

  private final JWTService jwtService;
  private final PasswordHashingService passwordHashingService;

  private final AuthenticationManager authManager;

  private final UserRepo userRepo;
  private final LearnerInfosRepo learnerInfosRepo;
  private final InstructorInfosRepo instructorInfosRepo;

  @Value("${application.default-values.user-profile-picture-filename}")
  private String defaultProfilePictureFilename;

  @Value("${application.default-values.default-storage-given}")
  private String defaultStorageGiven;

  public UserService(
      UserRepo userRepo, AuthenticationManager authManager, JWTService jwtService,
      LearnerInfosRepo learnerInfos, InstructorInfosRepo instructorInfos,
      PasswordHashingService passwordHashingService
  ) {
    this.userRepo = userRepo;
    this.authManager = authManager;
    this.jwtService = jwtService;
    this.learnerInfosRepo = learnerInfos;
    this.instructorInfosRepo = instructorInfos;
    this.passwordHashingService = passwordHashingService;
  }

  public Optional<Users> findUserById(UUID id) {
    return userRepo.findUsersById(id);
  }

  private Optional<String> basicRegisterRequestValidation(RegisterRequest user) {
    user.setUsername(user.getUsername().trim());
    user.setPassword(user.getPassword().trim());

    if (
        user.getUsername() == null || user.getPassword() == null || user.getAccountType() == null
            || user.getUsername().isBlank() || user.getPassword().isBlank()
    ) {
      return Optional.of("Username, password and account type must be provided for user registration");
    }
    if (user.getUsername().length() > 100) {
      return Optional.of("Username must be between 1 and 100 characters (Excluding leading or trailing whitespaces)");
    }
    if ((user.getEmail().length() < 5) || (user.getEmail().length() > 100)) {
      return Optional.of("E-mail must be between 5 and 100 characters (Excluding leading or trailing whitespaces)");
    }
    if ((user.getPassword().length() < 8) || (user.getPassword().length() > 128)) {
      return Optional.of("Password must be between 8 and 128 characters (Excluding leading or trailing whitespaces)");
    }
    boolean eitherOfTwoType = user.getAccountType().equalsIgnoreCase("Learner") || user.getAccountType().equalsIgnoreCase("Instructor");
    if (!eitherOfTwoType) {
      return Optional.of("Account type must be either Learner or Instructor");
    }

    return Optional.empty();
  }

  public Optional<String> register(RegisterRequest registerRequest) {
    Optional<String> invalidReason = basicRegisterRequestValidation(registerRequest);
    if (invalidReason.isPresent()) {
      return invalidReason;
    }

    Optional<Users> userInfoFromDb = userRepo.findUsersByEmail(registerRequest.getEmail());
    if (userInfoFromDb.isPresent()) {
      return Optional.of("A user with that e-mail already exists. Please try again using different e-mail.");
    }

    userInfoFromDb = userRepo.findUsersByUsername(registerRequest.getUsername());
    if (userInfoFromDb.isPresent()) {
      return Optional.of("A user with that username already exists. Please try again with different username.");
    }

    Users userToRegister = new Users();
    userToRegister.setUsername(registerRequest.getUsername());
    userToRegister.setPassword(passwordHashingService.getEncodedPassword(registerRequest.getPassword()));
    userToRegister.setUserType(registerRequest.getAccountType());
    userToRegister.setEmail(registerRequest.getEmail());
    userToRegister.setPfpPath(defaultProfilePictureFilename);
    userToRegister.setTotalStorageInBytes(Long.parseLong(defaultStorageGiven));
    userToRegister.setUsedStorageInBytes(0);

    if (registerRequest.getAccountType().equalsIgnoreCase("Learner")) {
      LearnerInfos savedLearnerInfo = learnerInfosRepo.save(
          LearnerInfos
              .builder()
              .myPurchasedCourses(new ArrayList<UUID>())
              .build()
      );
      userToRegister.setLearnerForeignKey(savedLearnerInfo.getId());
    } else {
      InstructorInfos savedInstructorInfo = instructorInfosRepo.save(
          InstructorInfos
              .builder()
              .myAddedCourses(new ArrayList<UUID>())
              .accountBalance(0.0)
              .totalCoursesSold(0)
              .build()
      );
      userToRegister.setInstructorForeignKey(savedInstructorInfo.getId());
    }
    userRepo.save(userToRegister);
    return Optional.empty();
  }

  public AuthenticationResult authenticate(LoginRequest loginRequest) {
    var authResult = AuthenticationResult.builder();

    CustomAuthToken customAuthToken = new CustomAuthToken(loginRequest.email(), loginRequest.password(), null);

    try {
      Authentication authentication = authManager.authenticate(customAuthToken);
      if (!authentication.isAuthenticated()) {
        return authResult
            .errorMessage("Authentication failed. Please try again.")
            .build();
      }

      Optional<Users> userInfoFromDb = userRepo.findUsersByEmail(loginRequest.email());
      if (userInfoFromDb.isEmpty()) {
        return authResult
            .errorMessage("No user is registered with that E-mail")
            .build();
      }

      String accessToken = jwtService.generateToken(
          userInfoFromDb.get().getEmail(),
          userInfoFromDb.get().getId().toString(),
          userInfoFromDb.get().getUserType(),
          JWTService.TokenType.AccessToken
      );

      String refreshToken = jwtService.generateToken(
          userInfoFromDb.get().getEmail(),
          userInfoFromDb.get().getId().toString(),
          userInfoFromDb.get().getUserType(),
          JWTService.TokenType.RefreshToken
      );

      return AuthenticationResult
          .builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .accountType(userInfoFromDb.get().getUserType())
          .errorMessage(null)
          .build();
    } catch (Exception e) {
      logger.error(e.getMessage());

      return authResult
          .errorMessage("Authentication failed.")
          .build();
    }
  }

  public Optional<String> changeUsername(String currentUserId, String newUsername) {
    Optional<Users> currentUser = userRepo.findUsersById(UUID.fromString(currentUserId));
    if (currentUser.isEmpty()) {
      return Optional.of("Failed to retrieve current user information.");
    }

    Optional<Users> newUsernameUser = userRepo.findUsersByUsername(newUsername);
    if (newUsernameUser.isPresent()) {
      return Optional.of("Username is already taken. A user already exists with that username.");
    }

    currentUser.get().setUsername(newUsername);
    try {
      userRepo.save(currentUser.get());
      return Optional.empty();
    } catch (Exception e) {
      logger.error("ERROR in `changeUsername`: {}", e.getMessage());

      return Optional.of("Failed to save the changed username.");
    }
  }

  public Optional<String> changePassword(String userId, String oldPassword, String newPassword) {
    Optional<Users> currentUser = userRepo.findUsersById(UUID.fromString(userId));
    if (currentUser.isEmpty()) {
      return Optional.of("Failed to retrieve current user information.");
    }

    if (!passwordHashingService.doesPasswordMatch(oldPassword, currentUser.get().getPassword())) {
      return Optional.of("The old password does not match the password in the database.");
    }

    String providedNewPasswordHashed = passwordHashingService.getEncodedPassword(newPassword);
    currentUser.get().setPassword(providedNewPasswordHashed);
    try {
      userRepo.save(currentUser.get());

      return Optional.empty();
    } catch (Exception e) {
      logger.error("ERROR in `changePassword()`: {}", e.getMessage());

      return Optional.of("Failed to save the changed password.");
    }
  }

  public Optional<String> updateProfilePicturePath(String userId, String newProfilePicturePath) {
    Optional<Users> currentUser = userRepo.findUsersById(UUID.fromString(userId));
    if (currentUser.isEmpty()) {
      return Optional.of("Failed to retrieve current user information.");
    }

    String oldPictureFilename = currentUser.get().getPfpPath();
    currentUser.get().setPfpPath(newProfilePicturePath);

    // if the old profile picture wasn't the default one, delete it after saving the new one's path
    if (!oldPictureFilename.equals(defaultProfilePictureFilename)) {
      try {
        Path oldProfilePicturePath = Paths.get(oldPictureFilename);
        boolean isDeleted = Files.deleteIfExists(oldProfilePicturePath);
        if (!isDeleted) {
          logger.warn("Failed to delete the old profile picture file: {}", oldProfilePicturePath);
        } else {
          logger.info("Successfully deleted the old profile picture file: {}", oldProfilePicturePath);
        }
      } catch (Exception e) {
        // Just log the error as this isn't a fatal error
        logger.warn("Failed to delete the old profile picture: {}", e.getMessage());
      }
    }

    try {
      userRepo.save(currentUser.get());
    } catch (Exception e) {
      logger.error("ERROR in `updateProfilePicturePath()`: {}", e.getMessage());

      return Optional.of("Failed to save the changed profile picture path.");
    }

    return Optional.empty();
  }
}
