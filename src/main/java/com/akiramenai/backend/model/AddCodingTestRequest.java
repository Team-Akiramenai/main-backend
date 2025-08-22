package com.akiramenai.backend.model;

import java.util.UUID;

public record AddCodingTestRequest(
    UUID courseId,

    String question,

    String expectedStdout
) {
}
