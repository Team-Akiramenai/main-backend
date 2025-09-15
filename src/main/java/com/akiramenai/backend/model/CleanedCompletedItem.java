package com.akiramenai.backend.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record CleanedCompletedItem(
    UUID associatedCourseId,
    String itemId,
    CourseItems itemType,
    String completedAt
) {
  public CleanedCompletedItem(CompletedCourseItems cci) {
    this(
        cci.getAssociatedCourseId(),
        cci.getItemId(),
        cci.getItemType(),
        cci.getCompletedAt().format(DateTimeFormatter.ISO_DATE)
    );
  }
}
