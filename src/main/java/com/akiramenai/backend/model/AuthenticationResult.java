package com.akiramenai.backend.model;

import lombok.Builder;

@Builder
public record AuthenticationResult(
    String errorMessage,
    String accessToken,
    String refreshToken,
    String accountType
) {
}
