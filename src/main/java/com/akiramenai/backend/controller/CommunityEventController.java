package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.AddCommunityEventRequest;
import com.akiramenai.backend.model.CommunityEvent;
import com.akiramenai.backend.repo.CommunityEventRepo;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class CommunityEventController {
  private final CommunityEventRepo communityEventRepo;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  public CommunityEventController(CommunityEventRepo communityEventRepo) {
    this.communityEventRepo = communityEventRepo;
  }

  @GetMapping("api/public/get/events")
  public void getCommunityEvents(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    List<CommunityEvent> communityEvents = communityEventRepo.findCommunityEventByEventDateAfter(
        LocalDate.now()
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


  @PostMapping("api/public/add/events")
  public void addCommunityEvents(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestBody AddCommunityEventRequest addCommunityEventRequest
  ) {
    if (addCommunityEventRequest.eventName() == null || addCommunityEventRequest.eventName().isBlank()) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Must provide eventName.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }
    if (addCommunityEventRequest.eventDescription() == null || addCommunityEventRequest.eventDescription().isBlank()) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Must provide eventDescription.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }
    if (addCommunityEventRequest.eventCoordinates() == null || addCommunityEventRequest.eventCoordinates().isBlank()) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Must provide eventCoordinates.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }
    if (addCommunityEventRequest.eventType() == null || addCommunityEventRequest.eventType().isBlank()) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Must provide eventType.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }
    if (addCommunityEventRequest.eventDate() == null || addCommunityEventRequest.eventDate().isBlank()) {
      httpResponseWriter.writeFailedResponse(
          httpResponse,
          "Must provide eventDate.",
          HttpStatus.BAD_REQUEST
      );
      return;
    }

    CommunityEvent communityEvent = CommunityEvent
        .builder()
        .eventName(addCommunityEventRequest.eventName())
        .eventDescription(addCommunityEventRequest.eventDescription())
        .eventCoordinates(addCommunityEventRequest.eventCoordinates())
        .eventType(addCommunityEventRequest.eventType())
        .eventDate(LocalDate.parse(addCommunityEventRequest.eventDate()))
        .createdAt(LocalDateTime.now())
        .lastModifiedAt(LocalDateTime.now())
        .build();
    communityEventRepo.save(communityEvent);

    httpResponseWriter.writeOkResponse(
        httpResponse,
        "Successfully added event to database.",
        HttpStatus.OK
    );
  }
}
