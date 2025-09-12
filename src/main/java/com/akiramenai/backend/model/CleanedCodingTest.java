package com.akiramenai.backend.model;

import java.util.UUID;

public record CleanedCodingTest(
    String itemId,
    UUID courseId,
    String question,
    String description,
    String expectedStdout
) {
  public CleanedCodingTest(CodingTest ct) {
    this(ct.getItemId(), ct.getCourseId(), ct.getQuestion(), ct.getDescription(), ct.getExpectedStdout());
  }
}
