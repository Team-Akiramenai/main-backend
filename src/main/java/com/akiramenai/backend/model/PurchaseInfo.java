package com.akiramenai.backend.model;

public record PurchaseInfo(
    String userId,
    PurchaseTypes purchaseTypes,
    String courseId,
    Integer storageToAddInGBs
) {
}
