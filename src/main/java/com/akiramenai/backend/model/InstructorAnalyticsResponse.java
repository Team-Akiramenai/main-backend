package com.akiramenai.backend.model;

import lombok.Builder;

@Builder
public record InstructorAnalyticsResponse(
    double accountBalance,
    long totalCoursesSold
) {
}
