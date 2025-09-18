package com.akiramenai.backend.model;

import java.nio.file.Path;

public record AddTerminalTestRequest(
    String courseId,
    String question,
    String description,

    Path evalScript
) {
}
