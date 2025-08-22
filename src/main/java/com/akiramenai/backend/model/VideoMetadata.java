package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@Table(name = "video_metadata")
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private UUID courseId;

  @NotBlank
  private String title;

  @NotBlank
  private String description;

  private String thumbnailImageName;

  @NotNull
  private UUID videoFileId;

  @NotNull
  private boolean isProcessing;

  private String subtitleFileName;

  @NotNull
  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime uploadDateTime;

  @NotNull
  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime lastModifiedDateTime;
}
