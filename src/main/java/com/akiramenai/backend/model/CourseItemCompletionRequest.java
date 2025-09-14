package com.akiramenai.backend.model;

import java.util.UUID;

public record CourseItemCompletionRequest(
    String courseId,
    String itemId
) {
}
