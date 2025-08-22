package com.akiramenai.backend.utility;

import com.akiramenai.backend.model.PolymorphicCredentials;
import com.akiramenai.backend.model.Users;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.service.JWTService;
import com.akiramenai.backend.service.PasswordHashingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomAuthProvider implements AuthenticationProvider {
  private final UserRepo userRepo;
  private final JWTService jwtService;
  private final PasswordHashingService passwordHashingService;

  public CustomAuthProvider(UserRepo userRepo, JWTService jwtService, PasswordHashingService passwordHashingService) {
    this.userRepo = userRepo;
    this.jwtService = jwtService;
    this.passwordHashingService = passwordHashingService;
  }

  @Override
  public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
    CustomAuthToken token = (CustomAuthToken) authentication;
    PolymorphicCredentials polyCreds = (PolymorphicCredentials) token.getCredentials();

    if (polyCreds.userEmail() != null && polyCreds.password() != null) {
      Optional<Users> targetUser = userRepo.findUsersByEmail(polyCreds.userEmail());
      if (targetUser.isEmpty()) {
        throw new BadCredentialsException("Authentication failed. Reason: User not found.");
      }

      if (!passwordHashingService.doesPasswordMatch(polyCreds.password(), targetUser.get().getPassword())) {
        throw new BadCredentialsException("Authentication failed. Reason: Password does not match.");
      }

      return new CustomAuthToken(polyCreds.userEmail(), polyCreds.password(), null, true);
    }

    if (polyCreds.jwtToken() != null) {
      Optional<String> failureReason = jwtService.isTokenExpired(polyCreds.jwtToken());
      if (failureReason.isPresent()) {
        throw new BadCredentialsException("Authentication failed. Reason: " + failureReason.get());
      }

      return new CustomAuthToken(null, null, polyCreds.jwtToken());
    }

    throw new BadCredentialsException("Authentication failed. Reason: Both user email and JWT token are null.");
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(CustomAuthToken.class);
  }
}