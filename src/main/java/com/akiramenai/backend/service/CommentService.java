package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CommentRepo;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CommentService {
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final CourseRepo courseRepo;
  private final CommentRepo commentRepo;

  public CommentService(CourseRepo courseRepo, CommentRepo commentRepo) {
    this.courseRepo = courseRepo;
    this.commentRepo = commentRepo;
  }

  public ResultOrError<String, BackendOperationErrors> getPaginatedComments(String courseId, int N, int pageNumber, Sort.Direction sorting) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();
    if (N < 1) {
      return resp
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("Invalid page size. Page size can't be less than one.")
          .build();
    }

    Optional<UUID> courseUUID = IdParser.parseId(courseId);
    if (courseUUID.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("Failed to parse courseId. Invalid courseId provided.")
          .build();
    }

    Page<Comment> page = commentRepo.findAllByCourseId(
        courseUUID.get(),
        PageRequest.of(pageNumber, N, Sort.by(sorting, "createdAt"))
    );

    List<CleanedComment> comments = new ArrayList<>();
    page.getContent().forEach(comment -> {
      comments.add(new CleanedComment(comment));
    });

    PaginatedComments pagedComments = PaginatedComments
        .builder()
        .retrievedCommentCount(page.getSize())
        .retrievedComments(comments)
        .pageNumber(pageNumber)
        .pageSize(page.getSize())
        .build();
    Optional<String> commentsJson = jsonSerializer.serialize(pagedComments);
    if (commentsJson.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .errorMessage("Failed to serialize response JSON.")
          .build();
    }

    return resp
        .result(commentsJson.get())
        .build();
  }

  public ResultOrError<String, BackendOperationErrors> addComment(AddCommentRequest request, UUID userId) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<UUID> courseId = IdParser.parseId(request.courseId());
    if (courseId.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("Failed to parse courseId. Invalid courseId provided.")
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(courseId.get());
    if (targetCourse.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.CourseNotFound)
          .errorMessage("Course not found.")
          .build();
    }

    LocalDateTime ldtNow = LocalDateTime.now();

    Comment commentToSave = Comment
        .builder()
        .authorId(userId)
        .courseId(courseId.get())
        .content(request.content())
        .createdAt(ldtNow)
        .lastModifiedAt(ldtNow)
        .build();

    try {
      commentRepo.save(commentToSave);

      Optional<String> commentIdJson = jsonSerializer.serialize(new ItemId(commentToSave.getId().toString()));
      if (commentIdJson.isEmpty()) {
        return res
            .errorType(BackendOperationErrors.FailedToSerializeJson)
            .errorMessage("Failed to serialize response JSON.")
            .build();
      }

      return res
          .result(commentIdJson.get())
          .build();
    } catch (Exception e) {
      log.error("Failed to save comment. Reason: {}.", e.getMessage());

      return res
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .errorMessage("Failed to save comment.")
          .build();
    }
  }

  public ResultOrError<String, BackendOperationErrors> modifyComment(ModifyCommentRequest request, UUID userId) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<UUID> commentId = IdParser.parseId(request.commentId());
    if (commentId.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("Failed to parse commentId. Invalid commentId provided.")
          .build();
    }

    Optional<Comment> targetComment = commentRepo.findCommentById(commentId.get());
    if (targetComment.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.ItemNotFound)
          .errorMessage("Comment not found.")
          .build();
    }

    if (!targetComment.get().getAuthorId().equals(userId)) {
      return res
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .errorMessage("Failed to modify comment. Attempted to modify other user's comment.")
          .build();
    }

    try {
      targetComment.get().setContent(request.content().trim());
      targetComment.get().setLastModifiedAt(LocalDateTime.now());
      commentRepo.save(targetComment.get());

      Optional<String> commentIdJson = jsonSerializer.serialize(new ItemId(targetComment.get().getId().toString()));
      if (commentIdJson.isEmpty()) {
        return res
            .errorType(BackendOperationErrors.FailedToSerializeJson)
            .errorMessage("Failed to serialize response JSON.")
            .build();
      }

      return res
          .result(commentIdJson.get())
          .build();
    } catch (Exception e) {
      log.error("Failed to save the modified comment. Reason: {}.", e.getMessage());

      return res
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .errorMessage("Failed to save the modified comment.")
          .build();
    }
  }

  public ResultOrError<String, BackendOperationErrors> deleteComment(ModifyCommentRequest request, UUID userId) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<UUID> commentId = IdParser.parseId(request.commentId());
    if (commentId.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("Failed to parse commentId. Invalid commentId provided.")
          .build();
    }

    Optional<Comment> targetComment = commentRepo.findCommentById(commentId.get());
    if (targetComment.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.ItemNotFound)
          .errorMessage("Comment not found.")
          .build();
    }

    if (!targetComment.get().getAuthorId().equals(userId)) {
      return res
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .errorMessage("Failed to remove comment. Attempted to remove other user's comment.")
          .build();
    }

    try {
      commentRepo.delete(targetComment.get());

      Optional<String> commentIdJson = jsonSerializer.serialize(new ItemId(request.commentId()));
      if (commentIdJson.isEmpty()) {
        return res
            .errorType(BackendOperationErrors.FailedToSerializeJson)
            .errorMessage("Failed to serialize response JSON.")
            .build();
      }

      return res
          .result(commentIdJson.get())
          .build();
    } catch (Exception e) {
      log.error("Failed to remove the comment. Reason: {}.", e.getMessage());

      return res
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .errorMessage("Failed to remove the comment.")
          .build();
    }
  }
}
