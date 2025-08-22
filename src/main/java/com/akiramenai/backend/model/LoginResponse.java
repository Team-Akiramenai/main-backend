package com.akiramenai.backend.model;

import lombok.Builder;

@Builder
public record LoginResponse(
    String accessToken,
    String accountType
) {
}
