package com.akiramenai.backend.model;

// The type of course item will be determined by the
// REST API endpoint that the request was sent to
public record DeleteCourseItemRequest(String courseId, String itemId) {
}
