package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
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

  @NotNull
  private List<String> tags;

  @NotNull
  private List<String> courseItemIds;

  @DecimalMin("1.0")
  @ColumnDefault("0.0")
  private double price;

  @NotNull
  @ColumnDefault("0")
  private Long totalStars;

  @NotNull
  @ColumnDefault("0")
  private Long usersWhoRatedCount;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime lastModifiedAt;

  @NotNull
  @ColumnDefault("false")
  private Boolean isPublished;

  @NotNull
  @ColumnDefault("false")
  private Boolean isHidden;
}
