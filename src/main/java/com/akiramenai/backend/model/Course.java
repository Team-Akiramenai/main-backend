package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // raw foreign key field
  @Column(name = "instructor_id", nullable = false)
  private UUID instructorId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "instructor_id", referencedColumnName = "id", insertable = false, updatable = false)
  private Users instructor; // We don't need to specify this during insertion. This get automatically handled
  // using the `instructorId` attribute declared above

  @NotBlank
  @Size(max = 200)
  private String title;

  @NotBlank
  @Size(max = 2000)
  private String description;

  private String thumbnailImageName;

  // ⚠️ check this later → you probably need @ElementCollection or a join table
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
