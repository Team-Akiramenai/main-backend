package com.akiramenai.backend.controller;


import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.*;
import com.akiramenai.backend.service.MediaStorageService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import com.akiramenai.backend.utility.VttHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static com.akiramenai.backend.model.FileUploadErrorTypes.*;

@Slf4j
@RestController
@RequestMapping("api/protected")
public class CourseItemController {
  private final MediaStorageService mediaStorageService;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  @Value("${application.default-values.media.subtitles-directory}")
  private String subtitlesDirectory;

  private final QuizRepo quizRepo;
  private final VideoMetadataRepo videoMetadataRepo;
  private final CodingTestRepo codingTestRepo;
  private final LearnerInfosRepo learnerInfosRepo;
  private final CompletedCourseItemsRepo completedCourseItemsRepo;

  public CourseItemController(QuizRepo quizRepo, VideoMetadataRepo videoMetadataRepo, CodingTestRepo codingTestRepo, LearnerInfosRepo learnerInfosRepo, CompletedCourseItemsRepo completedCourseItemsRepo, MediaStorageService mediaStorageService) {
    this.quizRepo = quizRepo;
    this.videoMetadataRepo = videoMetadataRepo;
    this.codingTestRepo = codingTestRepo;
    this.learnerInfosRepo = learnerInfosRepo;
    this.completedCourseItemsRepo = completedCourseItemsRepo;
    this.mediaStorageService = mediaStorageService;
  }

  @GetMapping("get/course-item")
  public void getCourseItem(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestParam(required = true) String itemId
  ) {
    UUID userId = UUID.fromString(httpRequest.getAttribute("userId").toString());
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

        boolean isItemCompleted = completedCourseItemsRepo.existsByLearnerIdAndItemId(userId, itemId);
        Optional<String> respJson = jsonSerializer.serialize(new CleanedQuiz(targetItem.get(), isItemCompleted));
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

        boolean isItemCompleted = completedCourseItemsRepo.existsByLearnerIdAndItemId(userId, itemId);
        Optional<String> respJson = jsonSerializer.serialize(new CleanedVideoMetadata(targetItem.get(), isItemCompleted));
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

        boolean isItemCompleted = completedCourseItemsRepo.existsByLearnerIdAndItemId(userId, itemId);
        Optional<String> respJson = jsonSerializer.serialize(new CleanedCodingTest(targetItem.get(), isItemCompleted));
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


  @GetMapping("get/transcription/{video-metadata-id}")
  public void getTranscription(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @PathVariable(name = "video-metadata-id") String videoMetadataId
  ) {
    Optional<ParsedItemInfo> parsedItemInfo = IdParser.parseItemId(videoMetadataId);
    if (parsedItemInfo.isEmpty() || parsedItemInfo.get().itemType() != CourseItems.Video) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Failed to parse videoMetadataId. Invalid videoMetadataId provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    Optional<VideoMetadata> videoMetadata = videoMetadataRepo.findVideoMetadataByItemId(videoMetadataId);
    if (videoMetadata.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to find videoMetadata.", HttpStatus.NOT_FOUND);
      return;
    }

    Path transcriptionFilePath = Paths.get(
        subtitlesDirectory,
        videoMetadata.get().getSubtitleFileName()
    );
    try {
      InputStream inputStream = new FileInputStream(transcriptionFilePath.toFile());
      httpResponse.setContentType("text/vtt");
      httpResponse.setStatus(HttpStatus.OK.value());
      StreamUtils.copy(inputStream, httpResponse.getOutputStream());
    } catch (Exception e) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to read transcription file.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }
  }

  @PostMapping("modify/transcription/{video-metadata-id}")
  public void modifyTranscription(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @PathVariable(name = "video-metadata-id") String videoMetadataItemId,
      @RequestParam(name = "modified-vtt", required = false) MultipartFile modifiedVtt
  ) {
    Optional<ParsedItemInfo> videoMetadataId = IdParser.parseItemId(videoMetadataItemId);
    if (videoMetadataId.isEmpty() || videoMetadataId.get().itemType() != CourseItems.Video) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Invalid videoMetadataId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<VideoMetadata> targetVM = videoMetadataRepo.findVideoMetadataByItemId(videoMetadataItemId);
    if (targetVM.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to find videoMetadata.", HttpStatus.NOT_FOUND);
      return;
    }

    if (modifiedVtt == null || modifiedVtt.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Invalid VTT file provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<Path, FileUploadErrorTypes> savedVttPath = mediaStorageService.saveVtt(modifiedVtt);
    if (savedVttPath.errorType() != null) {
      switch (savedVttPath.errorType()) {
        case UnsupportedFileType, FileIsEmpty -> {
          httpResponseWriter.writeFailedResponse(httpResponse, savedVttPath.errorMessage(), HttpStatus.BAD_REQUEST);
          return;
        }
        case InvalidUploadDir, FailedToCreateUploadDir, FailedToSaveFile -> {
          httpResponseWriter.writeFailedResponse(httpResponse, savedVttPath.errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
          return;
        }
      }
    }

    // check if it is a valid VTT file
    VttValidationResult validationResult = VttHelper.validateVttFile(savedVttPath.result().toAbsolutePath().toString());
    // if it isn't, then reject it and delete the temp vtt file
    if (!validationResult.isValid()) {
      if (!savedVttPath.result().toFile().delete()) {
        log.warn("Failed to delete temp VTT file: {}", savedVttPath.result().toAbsolutePath().toString());
      }
      httpResponseWriter.writeFailedResponse(httpResponse, "Invalid VTT file provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    // if it is, then delete the old vtt file and rename the temp vtt file to the real filename
    File oldVttFile = new File(
        subtitlesDirectory,
        targetVM.get().getSubtitleFileName()
    );
    if (!oldVttFile.delete()) {
      log.warn("Failed to delete temp VTT file: {}", oldVttFile.getAbsolutePath());
    }
    boolean isRenamed = savedVttPath.result().toFile().renameTo(oldVttFile);
    if (!isRenamed) {
      log.error("Failed to rename temp VTT file: {}", savedVttPath.result().toAbsolutePath().toString());

      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to save the modified VTT file.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(httpResponse, "Modified the VTT file.", HttpStatus.CREATED);
  }
}
