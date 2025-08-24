package com.akiramenai.backend.model;

import java.util.UUID;

public record CleanedComment(
    UUID courseId,
    UUID authorId,
    String content,
    String createdAt,
    String lastModifiedAt
) {
  public CleanedComment(Comment c) {
    this(
        c.getCourseId(),
        c.getAuthorId(),
        c.getContent(),
        c.getCreatedAt().toString(),
        c.getLastModifiedAt().toString()
    );
  }
}
