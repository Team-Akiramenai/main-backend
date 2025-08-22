package com.akiramenai.backend.model;

import io.jsonwebtoken.Claims;
import lombok.Builder;

@Builder
public record JwtClaimsOrError(
    Claims claims,
    String friendlyErrorMessage
) {
}
