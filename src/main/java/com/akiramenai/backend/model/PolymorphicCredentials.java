package com.akiramenai.backend.model;

public record PolymorphicCredentials(String userEmail, String password, String jwtToken) {
}
