package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class Course {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull
  private UUID instructorId;

  @NotBlank
  @Size(max = 200)
  private String title;

  @NotBlank
  @Size(max = 2000)
  private String description;

  private String thumbnailImageName;

  @NotNull
  private List<String> courseItemIds;

  @DecimalMin("1.0")
  private double price;

  @NotNull
  private Long totalStars;

  @NotNull
  private Long usersWhoRatedCount;

  @CreatedDate
  @NotNull
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @NotNull
  private LocalDateTime lastModifiedAt;

  @NotNull
  private Boolean isPublished;
}
