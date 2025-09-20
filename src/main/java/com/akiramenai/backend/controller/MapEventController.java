package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.CommunityEvent;
import com.akiramenai.backend.repo.CommunityEventRepo;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
public class MapEventController {
  private final CommunityEventRepo communityEventRepo;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  public MapEventController(CommunityEventRepo communityEventRepo) {
    this.communityEventRepo = communityEventRepo;
  }

  @GetMapping("api/public/get/events")
  public void getEvents(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    List<CommunityEvent> communityEvents = communityEventRepo.findCommunityEventByEventDateTimeAfter(
        LocalDateTime.now()
    );
    if (communityEvents == null) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Failed to retrieve community events.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
      return;
    }

    Optional<String> respJson = jsonSerializer.serialize(communityEvents);
    if (respJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Failed to serialize JSON response.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
      return;
    }

    httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
  }
}
