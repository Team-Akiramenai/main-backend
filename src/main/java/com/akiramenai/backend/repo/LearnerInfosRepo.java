package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.InstructorInfos;
import com.akiramenai.backend.model.LearnerInfos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerInfosRepo extends JpaRepository<LearnerInfos, UUID> {
  Optional<LearnerInfos> findLearnerInfosById(UUID id);

}
