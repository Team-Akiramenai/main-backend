package com.akiramenai.backend.model;

public record PurchaseCourseRequest(
    String courseId,
    String transactionId
) {
}
