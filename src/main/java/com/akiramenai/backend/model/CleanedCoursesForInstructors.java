package com.akiramenai.backend.model;

import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CleanedCoursesForInstructors(
    UUID id,
    UUID instructorId,
    String title,
    String description,
    String thumbnailImageName,
    List<String> tags,
    List<String> courseItemIds,
    double price,
    double rating,
    String createdAt,
    String lastModifiedAt,
    boolean isPublished
) {
  public CleanedCoursesForInstructors(Course course) {
    this(
        course.getId(),
        course.getInstructorId(),
        course.getTitle(),
        course.getDescription(),
        course.getThumbnailImageName(),
        course.getTags(),
        course.getCourseItemIds(),
        course.getPrice(),
        (course.getUsersWhoRatedCount() > 0L
            ? (double) course.getTotalStars() / (double) course.getUsersWhoRatedCount()
            : 0.0
        ),
        course.getCreatedAt().toString(),
        course.getLastModifiedAt().toString(),
        course.getIsPublished()
    );
  }
}
