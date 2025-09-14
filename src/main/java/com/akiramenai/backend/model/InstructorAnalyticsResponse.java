package com.akiramenai.backend.model;

import lombok.Builder;

@Builder
public record InstructorAnalyticsResponse(
    int loginStreak,
    double accountBalance,
    long totalCoursesSold
) {
}
