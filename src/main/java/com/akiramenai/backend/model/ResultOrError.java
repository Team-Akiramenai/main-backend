package com.akiramenai.backend.model;

import lombok.Builder;

@Builder
public record ResultOrError<T, U>(
    T result,
    String errorMessage,
    U errorType
) {
}
