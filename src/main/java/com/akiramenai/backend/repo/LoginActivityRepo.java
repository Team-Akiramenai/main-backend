package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.LoginActivity;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoginActivityRepo extends JpaRepository<LoginActivity, UUID> {
  Optional<LoginActivity> findLoginActivitiesByAssociatedUserIdAndYear(UUID associatedUser_id, @NotNull int year);
}
