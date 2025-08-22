package com.akiramenai.backend.model;

import lombok.Builder;

import java.util.List;

@Builder
public record PaginatedCourses(
    int retrievedCourseCount,
    List<CleanedCourse> retrievedCourses,

    int pageNumber,
    int pageSize
) {
}
