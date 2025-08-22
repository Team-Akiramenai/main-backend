package com.akiramenai.backend.model;

import java.util.UUID;

// The type of course item will be determined by the
// REST API endpoint that the request was sent to
public record DeleteCourseItemRequest(UUID courseId, UUID itemId) {
}
