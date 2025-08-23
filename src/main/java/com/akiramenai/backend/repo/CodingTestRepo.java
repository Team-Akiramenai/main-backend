package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.CodingTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CodingTestRepo extends JpaRepository<CodingTest, UUID> {
  Optional<CodingTest> findCodingTestById(UUID id);

  Optional<CodingTest> findCodingTestByItemId(String itemId);
}
