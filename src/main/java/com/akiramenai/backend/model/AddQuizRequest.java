package com.akiramenai.backend.model;

import java.util.UUID;

public record AddQuizRequest(
    UUID courseId,
    String question,
    String o1,
    String o2,
    String o3,
    String o4,
    Integer correctOption
) {
}
