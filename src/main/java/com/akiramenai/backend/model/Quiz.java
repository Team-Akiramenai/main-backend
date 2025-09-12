package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quizzes")
public class Quiz {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private String itemId;

  @NotNull
  private UUID courseId;

  @NotBlank
  private String question;

  @NotBlank
  private String o1;

  @NotBlank
  private String o2;

  @NotBlank
  private String o3;

  @NotBlank
  private String o4;

  @Min(1)
  @Max(4)
  private int correctOption;
}
