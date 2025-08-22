package com.akiramenai.backend.model;

public record RateCourseRequest(
    String courseId,
    int rating
) {
}
