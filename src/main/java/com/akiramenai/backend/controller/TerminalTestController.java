package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.service.StorageService;
import com.akiramenai.backend.service.TerminalTestService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@RestController
public class TerminalTestController {
  private final StorageService storageService;
  HttpResponseWriter responseWriter = new HttpResponseWriter();
  private final TerminalTestService terminalTestService;
  private final HttpResponseWriter httpResponseWriter = new HttpResponseWriter();

  public TerminalTestController(TerminalTestService terminalTestService, StorageService storageService) {
    this.terminalTestService = terminalTestService;
    this.storageService = storageService;
  }

  @PostMapping("api/protected/add/terminal-test")
  public void addTerminalTest(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam("course-id") String courseId,
      @RequestParam("question") String question,
      @RequestParam("description") String description,
      @RequestParam("eval-script") MultipartFile evalScript
  ) {
    if (courseId == null || question == null || description == null || evalScript == null) {
      responseWriter.writeFailedResponse(
          response,
          "The fields for courseId, question, description and evalScript must be provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    ResultOrError<Path, FileUploadErrorTypes> savedScriptPath = storageService.saveScript(evalScript);
    if (savedScriptPath.errorType() != null) {
      switch (savedScriptPath.errorType()) {
        case FileIsEmpty, UnsupportedFileType -> {
          responseWriter.writeFailedResponse(response, savedScriptPath.errorMessage(), HttpStatus.BAD_REQUEST);
        }
        case FailedToCreateUploadDir, InvalidUploadDir, FailedToSaveFile -> {
          responseWriter.writeFailedResponse(response, savedScriptPath.errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
      }
    }

    AddTerminalTestRequest req = new AddTerminalTestRequest(courseId, question, description, savedScriptPath.result());
    ResultOrError<String, BackendOperationErrors> resp = terminalTestService.addTerminalTest(
        req,
        UUID.fromString(request.getAttribute("userId").toString())
    );
    httpResponseWriter.handleDifferentResponses(response, resp, HttpStatus.CREATED);
  }

  @PostMapping("api/protected/modify/terminal-test")
  public void modifyTerminalTest(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestParam("course-id") String courseId,
      @RequestParam("item-id") String itemId,
      @RequestParam("question") String question,
      @RequestParam("description") String description,
      @RequestParam("new-script") MultipartFile newScript
  ) {
    if (courseId == null || itemId == null) {
      responseWriter.writeFailedResponse(
          httpResponse,
          "The courseId and itemId fields must be provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    ResultOrError<Path, FileUploadErrorTypes> savedScriptPath = null;
    if (newScript != null) {
      savedScriptPath = storageService.saveScript(newScript);
      if (savedScriptPath.errorType() != null) {
        switch (savedScriptPath.errorType()) {
          case FileIsEmpty, UnsupportedFileType -> {
            responseWriter.writeFailedResponse(httpResponse, savedScriptPath.errorMessage(), HttpStatus.BAD_REQUEST);
          }
          case FailedToCreateUploadDir, InvalidUploadDir, FailedToSaveFile -> {
            responseWriter.writeFailedResponse(httpResponse, savedScriptPath.errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
          }
        }
      }
    }

    ModifyTerminalTestRequest req = new ModifyTerminalTestRequest(
        courseId,
        itemId,
        question,
        description,
        (savedScriptPath == null) ? null : savedScriptPath.result()
    );
    ResultOrError<String, BackendOperationErrors> resp = terminalTestService.modifyTerminalTest(
        req,
        UUID.fromString(httpRequest.getAttribute("userId").toString())
    );
    httpResponseWriter.handleDifferentResponses(httpResponse, resp, HttpStatus.CREATED);
  }

  @PostMapping("api/protected/remove/terminal-test")
  public void removeTerminalTest(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestBody DeleteCourseItemRequest deleteCourseItemRequest
  ) {
    if (deleteCourseItemRequest.courseId() == null || deleteCourseItemRequest.itemId() == null) {
      responseWriter.writeFailedResponse(
          httpResponse,
          "The fields for courseId and itemId must be provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    ResultOrError<String, BackendOperationErrors> resp = terminalTestService.deleteTerminalTest(
        deleteCourseItemRequest,
        UUID.fromString(httpRequest.getAttribute("userId").toString())
    );

    httpResponseWriter.handleDifferentResponses(httpResponse, resp, HttpStatus.OK);
  }
}
