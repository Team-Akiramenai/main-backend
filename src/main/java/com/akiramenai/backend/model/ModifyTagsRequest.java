package com.akiramenai.backend.model;

import java.util.List;

public record ModifyTagsRequest(
    String courseId,
    List<String> tagsToBeModified
) {
}
