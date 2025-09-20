package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.CommunityEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommunityEventRepo extends JpaRepository<CommunityEvent, UUID> {
  List<CommunityEvent> findCommunityEventByEventDateTimeAfter(LocalDateTime eventDateTimeAfter);
}
