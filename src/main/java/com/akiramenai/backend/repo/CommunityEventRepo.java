package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.CommunityEvent;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommunityEventRepo extends JpaRepository<CommunityEvent, UUID> {
  List<CommunityEvent> findCommunityEventByEventDateAfter(@NotNull LocalDate eventDate);
}
