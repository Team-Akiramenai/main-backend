package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.AddQuizRequest;
import com.akiramenai.backend.service.MeiliService;
import com.akiramenai.backend.service.QuizService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.JsonSerializer;
import com.meilisearch.sdk.model.SearchResultPaginated;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
public class SearchController {
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final MeiliService meiliService;

  public SearchController(MeiliService meiliService) {
    this.meiliService = meiliService;
  }

  @GetMapping("api/public/search/courses")
  public void searchCourses(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam(required = false, defaultValue = "") String query,
      @RequestParam(name = "page-number", required = false, defaultValue = "1") String pageNumber,
      @RequestParam(name = "page-size", required = false, defaultValue = "10") String pageSize
  ) {
    Optional<SearchResultPaginated> paginatedResults = meiliService.searchCourses(
        query,
        Integer.parseInt(pageNumber),
        Integer.parseInt(pageSize)
    );
    if (paginatedResults.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Failed to search.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    Optional<String> jsonResp = jsonSerializer.serialize(paginatedResults.get());
    if (jsonResp.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(response, jsonResp.get(), HttpStatus.OK);
  }
}
