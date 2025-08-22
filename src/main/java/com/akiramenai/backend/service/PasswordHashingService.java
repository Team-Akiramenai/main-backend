package com.akiramenai.backend.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHashingService {
  @Value("${application.security.password.hashing-strength}")
  private String PasswordHashingStrength;

  BCryptPasswordEncoder encoder;

  public String getEncodedPassword(String password) {
    if (password == null) {
      throw new IllegalArgumentException("Password cannot be null.");
    }

    if (encoder == null) {
      encoder = new BCryptPasswordEncoder(Integer.parseInt(PasswordHashingStrength));
    }

    return encoder.encode(password);
  }

  public boolean doesPasswordMatch(@NotNull String password, @NotNull String hashedPassword) {
    return BCrypt.checkpw(password, hashedPassword);
  }
}
