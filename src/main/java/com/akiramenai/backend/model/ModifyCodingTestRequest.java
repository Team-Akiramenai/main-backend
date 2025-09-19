package com.akiramenai.backend.model;

public record ModifyCodingTestRequest(
    String courseId,
    String itemId,

    String question,
    String description,
    String input,
    String expectedStdout
) {
}
