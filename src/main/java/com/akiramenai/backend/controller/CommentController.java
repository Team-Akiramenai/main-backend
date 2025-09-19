package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.service.CommentService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class CommentController {
  private final CommentService commentService;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("api/protected/get/video/comments")
  public void getCommentsForVideo(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam(name = "video-id", required = true) String videoMetadataId,
      @RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "page-size", required = false, defaultValue = "5") int pageSize,
      @RequestParam(value = "sorting", required = false, defaultValue = "ASC") String sorting
  ) {
    if (videoMetadataId.isBlank()) {
      httpResponseWriter.writeFailedResponse(response, "Invalid VideoId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<ParsedItemInfo> parsedItemInfo = IdParser.parseItemId(videoMetadataId);
    if (parsedItemInfo.isEmpty() || parsedItemInfo.get().itemType() != CourseItems.VideoMetadata) {
      httpResponseWriter.writeFailedResponse(response, "Invalid VideoId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, BackendOperationErrors> paginatedComments = commentService.getCommentsForVideo(
        videoMetadataId,
        pageSize,
        page,
        (sorting.equalsIgnoreCase("DESC")) ? Sort.Direction.DESC : Sort.Direction.ASC
    );
    httpResponseWriter.handleDifferentResponses(response, paginatedComments, HttpStatus.OK);
  }


  @GetMapping("api/protected/get/my/comments")
  public void getCommentsByMe(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "page-size", required = false, defaultValue = "5") int pageSize,
      @RequestParam(value = "sorting", required = false, defaultValue = "ASC") String sorting
  ) {
    String currentUserId = request.getAttribute("userId").toString();
    Optional<UUID> parsedUserId = IdParser.parseId(currentUserId);
    if (parsedUserId.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Invalid user ID received.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, BackendOperationErrors> paginatedComments = commentService.getCommentsByAuthor(
        parsedUserId.get(),
        pageSize,
        page,
        (sorting.equalsIgnoreCase("DESC")) ? Sort.Direction.DESC : Sort.Direction.ASC
    );
    httpResponseWriter.handleDifferentResponses(response, paginatedComments, HttpStatus.OK);
  }

  @PostMapping("api/protected/set/comment")
  public void setComment(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody AddCommentRequest addCommentRequest
  ) {
    String currentUserId = request.getAttribute("userId").toString();

    ResultOrError<String, BackendOperationErrors> paginatedComments = commentService.addComment(
        addCommentRequest,
        UUID.fromString(currentUserId)
    );
    httpResponseWriter.handleDifferentResponses(response, paginatedComments, HttpStatus.CREATED);
  }

  @PostMapping("api/protected/modify/comment")
  public void modifyComment(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody ModifyCommentRequest modifyCommentRequest
  ) {
    String currentUserId = request.getAttribute("userId").toString();

    ResultOrError<String, BackendOperationErrors> paginatedComments = commentService.modifyComment(
        modifyCommentRequest,
        UUID.fromString(currentUserId)
    );
    httpResponseWriter.handleDifferentResponses(response, paginatedComments, HttpStatus.CREATED);
  }


  @PostMapping("api/protected/remove/comment")
  public void removeComment(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody DeleteCommentRequest deleteCommentRequest
  ) {
    String currentUserId = request.getAttribute("userId").toString();

    ResultOrError<String, BackendOperationErrors> paginatedComments = commentService.deleteComment(
        deleteCommentRequest,
        UUID.fromString(currentUserId)
    );
    httpResponseWriter.handleDifferentResponses(response, paginatedComments, HttpStatus.CREATED);
  }
}
