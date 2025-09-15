package com.akiramenai.backend.model;

import java.util.List;

public record DeleteTagsRequest(
    String courseId,
    List<String> tagsToBeDeleted
) {
}
