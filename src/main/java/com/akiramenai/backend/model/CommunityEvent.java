package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "community_events")
public class CommunityEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private String eventName;

  @NotNull
  private String eventDescription;

  @NotNull
  private String eventCoordinates;

  @NotNull
  private String eventType;

  @NotNull
  private LocalDate eventDate;

  @CreatedDate
  @NotNull
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @NotNull
  private LocalDateTime lastModifiedAt;
}
