package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.CompletedCourseItems;
import com.akiramenai.backend.model.CourseItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompletedCourseItemsRepo extends JpaRepository<CompletedCourseItems, UUID> {
  Optional<List<CompletedCourseItems>> findCompletedCourseItemsByLearnerIdAndItemType(UUID learnerId, CourseItems itemType);

  Optional<List<CompletedCourseItems>> findCompletedCourseItemsByLearnerIdAndItemTypeAndCompletedAtBetween(
      UUID learnerId,
      CourseItems itemType,
      LocalDate completedAtAfter,
      LocalDate completedAtBefore
  );

  boolean existsByLearnerIdAndItemId(UUID learnerId, String itemId);
}
