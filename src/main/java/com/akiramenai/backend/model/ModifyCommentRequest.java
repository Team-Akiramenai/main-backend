package com.akiramenai.backend.model;

public record ModifyCommentRequest(
    String commentId,
    String content
) {
}
