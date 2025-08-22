package com.akiramenai.backend.model;

public record LoginRequest(
    String email,
    String password
) {
}
