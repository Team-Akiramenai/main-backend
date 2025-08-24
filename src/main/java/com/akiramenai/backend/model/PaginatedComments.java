package com.akiramenai.backend.model;

import lombok.Builder;

import java.util.List;

@Builder
public record PaginatedComments(
    int retrievedCommentCount,
    List<CleanedComment> retrievedComments,

    int pageNumber,
    int pageSize
) {
}
