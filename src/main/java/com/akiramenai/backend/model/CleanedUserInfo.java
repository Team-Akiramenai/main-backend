package com.akiramenai.backend.model;

import java.util.UUID;

public record CleanedUserInfo(
    UUID userId,
    String username,
    String email,
    String userType,
    String pfpPath,
    long totalStorageInBytes,
    long usedStorageInBytes
) {
  public CleanedUserInfo(Users user) {
    this(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getUserType(),
        user.getPfpPath(),
        user.getTotalStorageInBytes(),
        user.getUsedStorageInBytes()
    );
  }
}
