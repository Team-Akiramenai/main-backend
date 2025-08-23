package com.akiramenai.backend.model;

import java.util.UUID;

public record AddCodingTestRequest(
    String courseId,

    String question,

    String expectedStdout
) {
}
