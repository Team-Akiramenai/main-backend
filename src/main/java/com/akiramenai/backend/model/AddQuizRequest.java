package com.akiramenai.backend.model;

public record AddQuizRequest(
    String courseId,
    String question,
    String o1,
    String o2,
    String o3,
    String o4,
    Integer correctOption) {
}
