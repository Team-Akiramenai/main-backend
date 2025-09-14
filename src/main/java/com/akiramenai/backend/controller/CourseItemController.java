package com.akiramenai.backend.controller;


import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.*;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("api/protected")
public class CourseItemController {
  private final QuizRepo quizRepo;
  private final VideoMetadataRepo videoMetadataRepo;
  private final CodingTestRepo codingTestRepo;
  private final LearnerInfosRepo learnerInfosRepo;
  private final CompletedCourseItemsRepo completedCourseItemsRepo;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  public CourseItemController(QuizRepo quizRepo, VideoMetadataRepo videoMetadataRepo, CodingTestRepo codingTestRepo, LearnerInfosRepo learnerInfosRepo, CompletedCourseItemsRepo completedCourseItemsRepo) {
    this.quizRepo = quizRepo;
    this.videoMetadataRepo = videoMetadataRepo;
    this.codingTestRepo = codingTestRepo;
    this.learnerInfosRepo = learnerInfosRepo;
    this.completedCourseItemsRepo = completedCourseItemsRepo;
  }

  @GetMapping("get/course-item")
  public void getCourseItem(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestParam(required = true) String itemId
  ) {
    Optional<ParsedItemInfo> itemInfo = IdParser.parseItemId(itemId);
    if (itemInfo.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to parse provided itemId.", HttpStatus.BAD_REQUEST);
      return;
    }

    switch (itemInfo.get().itemType()) {
      case Quiz -> {
        Optional<Quiz> targetItem = quizRepo.findQuizByItemId(itemId);
        if (targetItem.isEmpty()) {
          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to find course item.", HttpStatus.NOT_FOUND);
          return;
        }

        Optional<String> respJson = jsonSerializer.serialize(new CleanedQuiz(targetItem.get()));
        if (respJson.isEmpty()) {
          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize the response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
          return;
        }

        httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
      }
      case Video -> {
        Optional<VideoMetadata> targetItem = videoMetadataRepo.findVideoMetadataByItemId(itemId);
        if (targetItem.isEmpty()) {
          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to find course item.", HttpStatus.NOT_FOUND);
          return;
        }

        Optional<String> respJson = jsonSerializer.serialize(new CleanedVideoMetadata(targetItem.get()));
        if (respJson.isEmpty()) {
          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize the response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
          return;
        }

        httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
      }
      case CodingTest -> {
        Optional<CodingTest> targetItem = codingTestRepo.findCodingTestByItemId(itemId);
        if (targetItem.isEmpty()) {
          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to find course item.", HttpStatus.NOT_FOUND);
          return;
        }

        Optional<String> respJson = jsonSerializer.serialize(new CleanedCodingTest(targetItem.get()));
        if (respJson.isEmpty()) {
          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize the response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
          return;
        }

        httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
      }
      case TerminalTest -> {
        httpResponseWriter.writeFailedResponse(httpResponse, "TODO: Implement this...", HttpStatus.INTERNAL_SERVER_ERROR);
//        Optional<Quiz> targetQuiz = quizRepo.findQuizByItemId(courseItem.itemId());
//        if (targetQuiz.isEmpty()) {
//          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to find course item.", HttpStatus.NOT_FOUND);
//          return;
//        }
//
//        Optional<String> respJson = jsonSerializer.serialize(new CleanedQuiz(targetQuiz.get()));
//        if (respJson.isEmpty()) {
//          httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize the response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
//          return;
//        }
//
//        httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
      }
    }
  }

  @PostMapping("add/completed/course-item")
  public void addCompletedCourseItem(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestBody CourseItemCompletionRequest courseItemCompletionRequest
  ) {
    UUID userId = UUID.fromString(httpRequest.getAttribute("userId").toString());
    String accountType = httpRequest.getAttribute("accountType").toString();

    if (accountType.equals("Instructor")) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Instructors can't perform this action.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<UUID> courseId = IdParser.parseId(courseItemCompletionRequest.courseId());
    if (courseId.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to parse courseId. Invalid courseId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<ParsedItemInfo> itemInfo = IdParser.parseItemId(courseItemCompletionRequest.itemId());
    if (itemInfo.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to parse itemId. Invalid itemId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    ItemId resp = new ItemId(courseItemCompletionRequest.itemId());
    Optional<String> respJson = jsonSerializer.serialize(resp);
    if (respJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to encode response JSON.", HttpStatus.BAD_REQUEST);
      return;
    }

    // if the course item is already marked as completed
    if (completedCourseItemsRepo.existsByLearnerIdAndItemId(userId, courseItemCompletionRequest.itemId())) {
      httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
      return;
    }

    // if the course item haven't been marked as completed
    CompletedCourseItems completedItem = CompletedCourseItems
        .builder()
        .learnerId(userId)
        .associatedCourseId(courseId.get())
        .itemId(courseItemCompletionRequest.itemId())
        .itemType(itemInfo.get().itemType())
        .build();
    completedCourseItemsRepo.save(completedItem);
    httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.CREATED);
  }
}
