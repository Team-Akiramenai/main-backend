package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.InstructorInfos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstructorInfosRepo extends JpaRepository<InstructorInfos, UUID> {
  Optional<InstructorInfos> getInstructorInfosById(UUID id);
}
