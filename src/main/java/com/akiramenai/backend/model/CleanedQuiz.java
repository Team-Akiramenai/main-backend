package com.akiramenai.backend.model;

public record CleanedQuiz(
    String itemId,
    String courseId,
    String question,
    String option1,
    String option2,
    String option3,
    String option4,
    int correctOption
) {
  public CleanedQuiz(Quiz q) {
    this(
        q.getItemId(),
        q.getCourseId().toString(),
        q.getQuestion(),
        q.getOption1(),
        q.getOption2(),
        q.getOption3(),
        q.getOption4(),
        q.getCorrectOption()
    );
  }
}
