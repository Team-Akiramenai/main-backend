package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.service.CodingTestService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
public class CodingTestController {
  HttpResponseWriter responseWriter = new HttpResponseWriter();
  private final CodingTestService codingTestService;
  private final HttpResponseWriter httpResponseWriter = new HttpResponseWriter();

  public CodingTestController(CodingTestService codingTestService) {
    this.codingTestService = codingTestService;
  }

  @PostMapping("api/protected/add/coding-test")
  public void addCodingTest(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody AddCodingTestRequest addCodingTestRequest
  ) {
    if (
        addCodingTestRequest.courseId() == null
            || addCodingTestRequest.question() == null
            || addCodingTestRequest.description() == null
            || addCodingTestRequest.expectedStdout() == null
    ) {
      responseWriter.writeFailedResponse(
          response,
          "The fields for course ID, question and expected stdout must be provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    ResultOrError<String, BackendOperationErrors> resp = codingTestService.addCodingTest(
        addCodingTestRequest,
        UUID.fromString(request.getAttribute("userId").toString())
    );
    if (resp.errorType() == null) {
      responseWriter.writeOkResponse(response, resp.result(), HttpStatus.CREATED);
      return;
    }

    switch (resp.errorType()) {
      case CourseNotFound -> {
        responseWriter.writeFailedResponse(response, resp.errorMessage(), HttpStatus.NOT_FOUND);
        return;
      }
      case FailedToSerializeJson, FailedToSaveToDb -> {
        responseWriter.writeFailedResponse(response, resp.errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return;
      }
      case AttemptingToModifyOthersItem -> {
        responseWriter.writeFailedResponse(response, resp.errorMessage(), HttpStatus.UNAUTHORIZED);
        return;
      }
    }
  }

  @PostMapping("api/protected/modify/coding-test")
  public void modifyCodingTest(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestBody ModifyCodingTestRequest modifyCodingTestRequest
  ) {
    if (modifyCodingTestRequest.courseId() == null || modifyCodingTestRequest.itemId() == null) {
      responseWriter.writeFailedResponse(
          httpResponse,
          "The courseId and itemUUID fields must be provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    ResultOrError<String, BackendOperationErrors> resp = codingTestService.modifyCodingTest(
        modifyCodingTestRequest,
        UUID.fromString(httpRequest.getAttribute("userId").toString())
    );

    httpResponseWriter.handleDifferentResponses(httpResponse, resp, HttpStatus.OK);
  }

  @PostMapping("api/protected/remove/coding-test")
  public void removeCodingTest(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestBody DeleteCourseItemRequest deleteCourseItemRequest
  ) {
    if (deleteCourseItemRequest.courseId() == null || deleteCourseItemRequest.itemId() == null) {
      responseWriter.writeFailedResponse(
          httpResponse,
          "The fields for courseId and itemUUID must be provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    ResultOrError<String, BackendOperationErrors> resp = codingTestService.deleteCodingTest(
        deleteCourseItemRequest,
        UUID.fromString(httpRequest.getAttribute("userId").toString())
    );

    httpResponseWriter.handleDifferentResponses(httpResponse, resp, HttpStatus.OK);
  }
}
