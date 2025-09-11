package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.Comment;
import com.akiramenai.backend.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepo extends JpaRepository<Comment, UUID> {
  Optional<Comment> findCommentById(UUID id);

  Page<Comment> findAllByVideoMetadataId(String videoMetadataId, Pageable pageable);

  Page<Comment> findAllByAuthorId(UUID authorId, Pageable pageable);
}
