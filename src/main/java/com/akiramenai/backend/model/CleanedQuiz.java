package com.akiramenai.backend.model;

public record CleanedQuiz(
    String itemId,
    String courseId,
    String question,
    String o1,
    String o2,
    String o3,
    String o4,
    int correctOption
) {
  public CleanedQuiz(Quiz q) {
    this(
        q.getItemId(),
        q.getCourseId().toString(),
        q.getQuestion(),
        q.getO1(),
        q.getO2(),
        q.getO3(),
        q.getO4(),
        q.getCorrectOption()
    );
  }
}
