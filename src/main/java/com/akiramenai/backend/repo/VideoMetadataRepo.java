package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoMetadataRepo extends JpaRepository<VideoMetadata, UUID> {
  Optional<VideoMetadata> findVideoMetadataById(UUID id);

  Optional<VideoMetadata> findVideoMetadataByItemId(String itemId);
}
