package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CommentRepo;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.repo.VideoMetadataRepo;
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
  private final VideoMetadataRepo videoMetadataRepo;
  private final UserRepo userRepo;
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final CommentRepo commentRepo;

  public CommentService(CommentRepo commentRepo, VideoMetadataRepo videoMetadataRepo, UserRepo userRepo) {
    this.commentRepo = commentRepo;
    this.videoMetadataRepo = videoMetadataRepo;
    this.userRepo = userRepo;
  }

  public ResultOrError<String, BackendOperationErrors> getCommentsForVideo(
      String videoMetadataId,
      int N, int pageNumber,
      Sort.Direction sorting
  ) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();
    if (N < 1) {
      return resp
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("Invalid page size. Page size can't be less than one.")
          .build();
    }

    Optional<VideoMetadata> targetVM = videoMetadataRepo.findVideoMetadataByItemId(videoMetadataId);
    if (targetVM.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.ItemNotFound)
          .errorMessage("Video metadata not found for provided ID.")
          .build();
    }

    Page<Comment> page = commentRepo.findAllByVideoMetadataId(
        videoMetadataId,
        PageRequest.of(pageNumber, N, Sort.by(sorting, "createdAt"))
    );

    List<CleanedComment> comments = new ArrayList<>();
    page.getContent().forEach(comment -> {
      Optional<Users> targetUser = userRepo.findUsersById(comment.getAuthorId());
      if (targetUser.isEmpty()) {
        return;
      }

      var cc = CleanedComment
          .builder()
          .commentId(comment.getId().toString())
          .authorName(targetUser.get().getUsername())
          .authorProfilePicture(targetUser.get().getPfpFileName())
          .content(comment.getContent())
          .createdAt(comment.getCreatedAt().toString())
          .lastModifiedAt(comment.getLastModifiedAt().toString());

      comments.add(cc.build());
    });

    PaginatedComments pagedComments = PaginatedComments
        .builder()
        .retrievedCommentCount(page.getNumberOfElements())
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


  public ResultOrError<String, BackendOperationErrors> getCommentsByAuthor(
      UUID authorId,
      int N, int pageNumber,
      Sort.Direction sorting
  ) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();
    if (N < 1) {
      return resp
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("Invalid page size. Page size can't be less than one.")
          .build();
    }

    Page<Comment> page = commentRepo.findAllByAuthorId(
        authorId,
        PageRequest.of(pageNumber, N, Sort.by(sorting, "createdAt"))
    );

    List<CleanedComment> comments = new ArrayList<>();
    page.getContent().forEach(comment -> {
      Optional<Users> targetUser = userRepo.findUsersById(comment.getAuthorId());
      if (targetUser.isEmpty()) {
        return;
      }

      var cc = CleanedComment
          .builder()
          .commentId(comment.getId().toString())
          .authorName(targetUser.get().getUsername())
          .authorProfilePicture(targetUser.get().getPfpFileName())
          .content(comment.getContent())
          .createdAt(comment.getCreatedAt().toString())
          .lastModifiedAt(comment.getLastModifiedAt().toString());

      comments.add(cc.build());
    });

    PaginatedComments pagedComments = PaginatedComments
        .builder()
        .retrievedCommentCount(page.getNumberOfElements())
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

  public ResultOrError<String, BackendOperationErrors> addComment(
      AddCommentRequest request,
      UUID userId
  ) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<Users> targetUser = userRepo.findUsersById(userId);
    if (targetUser.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.CourseNotFound)
          .errorMessage("User not found.")
          .build();
    }

    Optional<VideoMetadata> targetVM = videoMetadataRepo.findVideoMetadataByItemId(request.videoMetadataId());
    if (targetVM.isEmpty()) {
      return res
          .errorType(BackendOperationErrors.CourseNotFound)
          .errorMessage("Video not found.")
          .build();
    }

    LocalDateTime ldtNow = LocalDateTime.now();
    Comment commentToSave = Comment
        .builder()
        .authorId(userId)
        .videoMetadataId(request.videoMetadataId())
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

  public ResultOrError<String, BackendOperationErrors> deleteComment(
      DeleteCommentRequest request,
      UUID userId
  ) {
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
