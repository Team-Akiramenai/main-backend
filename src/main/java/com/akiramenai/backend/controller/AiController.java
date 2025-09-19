package com.akiramenai.backend.controller;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import com.akiramenai.backend.model.*;

import com.akiramenai.backend.repo.PurchaseRepo;
import com.akiramenai.backend.service.AiService;

import com.akiramenai.backend.utility.HttpResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {
  private final PurchaseRepo purchaseRepo;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();

  private final AiService aiService;

  @Autowired
  public AiController(AiService aiService, PurchaseRepo purchaseRepo) {
    this.aiService = aiService;
    this.purchaseRepo = purchaseRepo;
  }

  @PostMapping("api/protected/suggest/course")
  public ResponseEntity<AiSuggestResponse> getCourseSuggestions(@RequestBody AiSuggestRequest req) {
    try {
      Optional<AiSuggestResponse> resp = this.aiService.getCourseSuggestions(req);
      if (resp.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      return ResponseEntity.ok(resp.get());
    } catch (IOException e) {
      return ResponseEntity.badRequest().build();
    }
  }


  @PostMapping("api/protected/get/video/help")
  public ResponseEntity<AiHelpResponse> getHelp(@RequestBody VideoAiHelpRequest req) {
    try {
      Optional<AiHelpResponse> resp = this.aiService.getHelp(req);
      if (resp.isEmpty()) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      return ResponseEntity.ok(resp.get());
    } catch (IOException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PostMapping("api/protected/get/generic/help")
  public void getGenericAiResponse(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestBody GenericAiRequest genericAiRequest
  ) {
    UUID userId = UUID.fromString(httpRequest.getParameter("userId"));

    int userCoursePuchaseCount = purchaseRepo.countPurchaseByBuyerId(userId);
    if (userCoursePuchaseCount <= 0) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Learner can use AI tools once they've bought at least one course.",
          HttpStatus.UNAUTHORIZED
      );
      return;
    }

    if (
        genericAiRequest.context() == null || genericAiRequest.context().isEmpty()
            || genericAiRequest.question() == null || genericAiRequest.question().isEmpty()
    ) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Both context and question fields must be provided.",
          HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<GenericAiResponse> aiResponse = aiService.getGenericResponse(genericAiRequest);
    if (aiResponse.isEmpty()) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Failed to retrieve AI response.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
      return;
    }
  }
}
