package com.akiramenai.backend.model;

public record StorageInfoResponse(long totalStorageBytes, long usedStorageBytes, double usagePercentage) {
}
