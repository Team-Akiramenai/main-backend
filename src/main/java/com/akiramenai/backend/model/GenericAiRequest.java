package com.akiramenai.backend.model;

public record GenericAiRequest(
    String context,
    String question
) {
}
