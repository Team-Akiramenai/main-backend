package com.akiramenai.backend.model;

public record AddCommentRequest(
    String courseId,
    String content
) {
}
