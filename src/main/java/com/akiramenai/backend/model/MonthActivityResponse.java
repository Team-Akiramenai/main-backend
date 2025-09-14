package com.akiramenai.backend.model;

import java.util.List;

public record MonthActivityResponse(
    List<Integer> activityInMonth
) {
}
