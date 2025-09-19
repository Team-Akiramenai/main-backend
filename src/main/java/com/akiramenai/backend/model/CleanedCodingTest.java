package com.akiramenai.backend.model;

import java.util.UUID;

public record CleanedCodingTest(
    String itemId,
    UUID courseId,
    String question,
    String description,
    String input,
    String expectedStdout,
    boolean isCompleted
) {
  public CleanedCodingTest(CodingTest ct, boolean isCompleted) {
    this(
        ct.getItemId(),
        ct.getCourseId(),
        ct.getQuestion(),
        ct.getDescription(),
        ct.getInput(),
        ct.getExpectedStdout(),
        isCompleted
    );
  }
}
