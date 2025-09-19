package com.akiramenai.backend.model;

public record CleanedTerminalTest(
    String itemId,
    String courseId,
    String question
) {
  public CleanedTerminalTest(TerminalTest terminalTest) {
    this(
        terminalTest.getItemId(),
        terminalTest.getCourseId().toString(),
        terminalTest.getQuestion()
    );
  }
}
