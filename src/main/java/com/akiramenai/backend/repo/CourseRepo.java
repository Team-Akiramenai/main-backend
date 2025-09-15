package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.Course;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepo extends JpaRepository<Course, UUID> {
  Optional<Course> findCourseById(UUID id);

  Page<Course> findAllByIsPublished(Boolean isPublished, Pageable pageable);

  Page<Course> findAllByInstructorId(UUID instructorId, Pageable pageable);

  Page<Course> findCourseById(UUID instructorId, Pageable pageable);

  Optional<Course> findCourseByTitle(String title);
}
