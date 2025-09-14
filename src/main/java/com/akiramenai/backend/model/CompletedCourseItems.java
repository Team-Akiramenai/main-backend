package com.akiramenai.backend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "completed_course_items")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompletedCourseItems {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, updatable = false)
  private UUID learnerId;

  @Column(nullable = false, updatable = false)
  private UUID associatedCourseId;

  @Column(nullable = false, updatable = false)
  private String itemId;

  @Column(nullable = false, updatable = false)
  private CourseItems itemType;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDate completedAt;
}
