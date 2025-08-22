package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchases")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Purchase {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private UUID courseId;

  @NotNull
  private UUID authorId;

  @NotNull
  private UUID buyerId;

  @NotNull
  private UUID transactionId;

  @NotNull
  private double price;

  @NotNull
  @Temporal(TemporalType.DATE)
  private LocalDate purchaseDate;

  @NotNull
  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime purchaseTimestamp;
}