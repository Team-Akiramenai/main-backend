package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepo extends JpaRepository<Quiz, UUID> {
  Optional<Quiz> findQuizById(UUID id);

  Optional<Quiz> findQuizByItemId(String itemId);
}
