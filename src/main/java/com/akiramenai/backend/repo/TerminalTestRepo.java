package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.TerminalTest;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TerminalTestRepo extends JpaRepository<TerminalTest, UUID> {
  Optional<TerminalTest> findTerminalTestByItemId(@NotNull String itemId);
}
