package com.akiramenai.backend.model;

public record ModifyQuizRequest(
    String courseId,
    String itemId,
    String question,
    String o1,
    String o2,
    String o3,
    String o4,
    Integer correctOption
) {
}
