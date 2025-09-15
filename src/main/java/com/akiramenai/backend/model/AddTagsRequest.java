package com.akiramenai.backend.model;

import java.util.List;

public record AddTagsRequest(
    String courseId,
    List<String> tagsToBeAdded
) {
}
