package com.akiramenai.backend.model;

import java.util.UUID;

public record ParsedItemInfo(CourseItems itemType, UUID itemUUID) {
}
