package com.akiramenai.backend.model;

public record ModifyQuizRequest(
    String courseId,
    String quizId,
    String question,
    String option1,
    String option2,
    String option3,
    String option4,
    Integer correctOption
) {
}
