package com.akiramenai.backend.model;

public record ModifyCodingTestRequest(
    String courseId,
    String codingTestId,
    String question,
    String expectedStdout
) {
}
