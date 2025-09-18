package com.akiramenai.backend.model;

import jakarta.persistence.*;
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
@Table(name = "terminal_tests")
public class TerminalTest {
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
  private String description;

  @NotBlank
  private String verificationScriptFilename;
}
