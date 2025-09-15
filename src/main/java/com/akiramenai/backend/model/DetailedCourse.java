package com.akiramenai.backend.model;


import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record DetailedCourse(
    UUID id,
    UUID instructorId,
    String instructorName,
    String title,
    String description,
    String thumbnailImageName,
    List<String> tags,
    List<String> courseItemIds,
    double price,
    double rating,
    long voterCount,
    long courseSoldCount,
    String createdAt,
    String lastModifiedAt
) {
  public DetailedCourse(Course course, long courseSoldCount) {
    this(
        course.getId(),
        course.getInstructorId(),
        course.getInstructor().getUsername(),
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
        course.getUsersWhoRatedCount(),
        courseSoldCount,
        course.getCreatedAt().toString(),
        course.getLastModifiedAt().toString()
    );
  }
}
