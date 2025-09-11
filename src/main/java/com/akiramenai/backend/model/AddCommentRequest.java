package com.akiramenai.backend.model;

public record AddCommentRequest(
    String videoMetadataId,
    String content
) {
}
