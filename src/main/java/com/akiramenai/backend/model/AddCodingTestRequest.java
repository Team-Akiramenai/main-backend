package com.akiramenai.backend.model;

public record AddCodingTestRequest(
    String courseId,
    String question,
    String description,
    String input,
    String expectedStdout
) {
}
