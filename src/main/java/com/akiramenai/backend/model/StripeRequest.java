package com.akiramenai.backend.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record StripeRequest(
    String productName,
    String productDescription,
    double cost,

    String buyerId,
    PurchaseTypes itemType,
    String courseId,
    Integer storageAmountInGBs
) {
}
