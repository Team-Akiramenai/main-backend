package com.akiramenai.backend.model;

import java.util.List;

public record ItemOrderUpdateRequest(String courseId, List<String> orderOfItemIds) {
}
