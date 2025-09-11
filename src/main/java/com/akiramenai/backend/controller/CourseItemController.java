package com.akiramenai.backend.controller;


import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CodingTestRepo;
import com.akiramenai.backend.repo.QuizRepo;
import com.akiramenai.backend.repo.VideoMetadataRepo;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("api/protected")
public class CourseItemController {
  private final QuizRepo quizRepo;
  private final VideoMetadataRepo videoMetadataRepo;
  private final CodingTestRepo codingTestRepo;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  public CourseItemController(QuizRepo quizRepo, VideoMetadataRepo videoMetadataRepo, CodingTestRepo codingTestRepo) {
    this.quizRepo = quizRepo;
    this.videoMetadataRepo = videoMetadataRepo;
    this.codingTestRepo = codingTestRepo;
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
}
