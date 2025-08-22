package com.akiramenai.backend.model;

import lombok.Builder;

@Builder
public record CourseSellDatapoint(
    String date,
    long coursesSold,
    double revenueGenerated
) {
}
