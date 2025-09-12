package com.akiramenai.backend.controller;

import java.io.IOException;
import java.util.Optional;

import com.akiramenai.backend.model.AiHelpRequest;
import com.akiramenai.backend.model.AiHelpResponse;
import com.akiramenai.backend.model.AiSuggestRequest;
import com.akiramenai.backend.model.AiSuggestResponse;

import com.akiramenai.backend.service.AiService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {
  private final AiService aiService;

  @Autowired
  public AiController(AiService aiService) {
    this.aiService = aiService;
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


  @PostMapping("api/protected/get/help")
  public ResponseEntity<AiHelpResponse> getHelp(@RequestBody AiHelpRequest req) {
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
}
