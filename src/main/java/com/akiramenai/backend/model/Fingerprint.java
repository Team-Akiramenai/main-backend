package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fingerprints")
public class Fingerprint {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private String videoMetadataId;

  @NotNull
  private UUID authorId;

  @NotNull
  private String audioDuration;

  @NotNull
  @Column(columnDefinition = "TEXT")
  private String audioFingerprint;
}
