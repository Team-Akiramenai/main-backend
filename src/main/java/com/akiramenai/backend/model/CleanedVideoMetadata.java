package com.akiramenai.backend.model;

import java.util.UUID;

public record CleanedVideoMetadata(
    String itemId,
    UUID courseId,

    String title,
    String description,
    String thumbnailImageName,
    UUID videoFileId,
    boolean isProcessing,
    String subtitleFileName,
    String uploadDateTime,
    String lastModifiedDateTime,
    boolean isCompleted
) {
  public CleanedVideoMetadata(VideoMetadata vm, boolean isCompleted) {
    this(
        vm.getItemId(),
        vm.getCourseId(),
        vm.getTitle(),
        vm.getDescription(),
        vm.getThumbnailImageName(),
        vm.getVideoFileId(),
        vm.isProcessing(),
        vm.getSubtitleFileName(),
        vm.getUploadDateTime().toString(),
        vm.getLastModifiedDateTime().toString(),
        isCompleted
    );
  }
}
