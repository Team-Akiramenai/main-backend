package com.akiramenai.backend.filters;

import com.akiramenai.backend.model.JwtErrorTypes;
import com.akiramenai.backend.model.ResultOrError;
import com.akiramenai.backend.service.JWTService;
import com.akiramenai.backend.utility.CustomAuthToken;
import com.akiramenai.backend.utility.RefreshTokenHandler;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {
  private final JWTService jwtService;

  public JwtFilter(JWTService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    Optional<String> refreshToken = RefreshTokenHandler.getFromCookie(request);

    Optional<Claims> refreshTokenClaims = Optional.empty();
    if (refreshToken.isPresent()) {
      ResultOrError<Claims, JwtErrorTypes> extractedClaims = jwtService.extractClaim(refreshToken.get());
      if (extractedClaims.errorType() == null) {
        refreshTokenClaims = Optional.of(extractedClaims.result());
      }
    }

    Optional<Claims> accessTokenClaims = Optional.empty();
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      Optional<String> extractedJwtToken = JWTService.extractTokenFromAuthHeader(authHeader);
      if (extractedJwtToken.isPresent()) {
        ResultOrError<Claims, JwtErrorTypes> extracted = jwtService.extractClaim(extractedJwtToken.get());
        if (extracted.errorType() == null) {
          accessTokenClaims = Optional.of(extracted.result());
        }
      }
    }

    Claims validClaims = null;
    if (accessTokenClaims.isEmpty()) {
      if (refreshTokenClaims.isPresent()) {
        Claims c = refreshTokenClaims.get();
        Optional<String> newAccessToken = jwtService.generateTokenUsingRefreshToken(
            refreshToken.get(), c.getSubject(), c.get("userId", String.class), c.get("accountType", String.class)
        );
        newAccessToken.ifPresent(newToken -> request.setAttribute("newAccessToken", newToken));
        newAccessToken.ifPresent(newToken -> response.setHeader("Refreshed-Token", "Bearer " + newToken));

        validClaims = c;
      } else {
        filterChain.doFilter(request, response);
        return;
      }
    } else {
      validClaims = accessTokenClaims.get();
    }

    request.setAttribute("userId", validClaims.get("userId", String.class));
    request.setAttribute("userEmail", validClaims.getSubject());
    request.setAttribute("accountType", validClaims.get("accountType", String.class));

    String token = authHeader;
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      Object newAccessToken = request.getAttribute("newAccessToken");
      if (newAccessToken != null) {
        logger.info("Generated new access token due to expired or missing access token.");
        token = newAccessToken.toString();
      }

      Optional<String> invalidReason = jwtService.isTokenValid(token);
      if (invalidReason.isPresent()) {
        filterChain.doFilter(request, response);
        return;
      }

      CustomAuthToken customAuthToken = new CustomAuthToken(null, null, token, true);
      SecurityContextHolder.getContext().setAuthentication(customAuthToken);
    }

    filterChain.doFilter(request, response);
  }
}
