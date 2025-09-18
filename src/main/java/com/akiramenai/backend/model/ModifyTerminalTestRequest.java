package com.akiramenai.backend.model;

import java.nio.file.Path;

public record ModifyTerminalTestRequest(
    String courseId,
    String itemId,

    String question,
    String description,
    Path newScript
) {
}
