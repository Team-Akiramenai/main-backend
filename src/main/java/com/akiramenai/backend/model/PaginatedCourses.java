package com.akiramenai.backend.model;

import lombok.Builder;

import java.util.List;

@Builder
public record PaginatedCourses<T>(
    int retrievedCourseCount,
    List<T> retrievedCourses,

    int pageNumber,
    int pageSize,
    int totalPaginatedPages
) {
}
