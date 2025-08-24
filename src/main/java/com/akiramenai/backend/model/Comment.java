package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "comment")
public class Comment {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private UUID courseId;

  @NotNull
  private UUID authorId;

  @NotBlank
  private String content;

  @CreatedDate
  @NotNull
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @NotNull
  private LocalDateTime lastModifiedAt;
}
