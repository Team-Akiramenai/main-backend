package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.service.QuizService;
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
public class QuizController {
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  private final QuizService quizService;

  public QuizController(QuizService quizService) {
    this.quizService = quizService;
  }

  @PostMapping("api/protected/add/quiz")
  public void addQuiz(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody AddQuizRequest addQuizRequest
  ) {
    ResultOrError<String, CourseItemOperationErrors> resp = quizService.addQuiz(
        addQuizRequest,
        UUID.fromString(request.getAttribute("userId").toString())
    );

    httpResponseWriter.handleDifferentResponses(response, resp, HttpStatus.CREATED);
  }


  @PostMapping("api/protected/modify/quiz")
  public void modifyQuiz(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody ModifyQuizRequest modifyQuizRequest
  ) {
    if (modifyQuizRequest.quizId() == null || modifyQuizRequest.courseId() == null) {
      httpResponseWriter.writeFailedResponse(
          response,
          "Request requires both quiz ID and course ID to be provided.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    ResultOrError<String, CourseItemOperationErrors> resp = quizService.modifyQuiz(
        modifyQuizRequest,
        UUID.fromString(request.getAttribute("userId").toString())
    );
    httpResponseWriter.handleDifferentResponses(response, resp, HttpStatus.OK);
  }


  @PostMapping("api/protected/remove/quiz")
  public void removeQuiz(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody DeleteCourseItemRequest deleteCourseItemRequest
  ) {
    ResultOrError<String, CourseItemOperationErrors> resp = quizService.removeQuiz(
        deleteCourseItemRequest,
        UUID.fromString(request.getAttribute("userId").toString())
    );

    httpResponseWriter.handleDifferentResponses(response, resp, HttpStatus.OK);
  }
}
