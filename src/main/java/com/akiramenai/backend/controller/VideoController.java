package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.service.VideoMetadataService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/protected/video-metadata")
public class VideoController {
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  VideoMetadataService videoMetadataService;

  public VideoController(
      VideoMetadataService videoMetadataService
  ) {
    this.videoMetadataService = videoMetadataService;
  }

  @PostMapping("/modify")
  public void modifyVideoMetadata(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestBody ModifyVideoMetadataRequest modifyVideoMetadataRequest
  ) {
    if (!request.getAttribute("accountType").equals("Instructor")) {
      httpResponseWriter.writeFailedResponse(response, "Only instructors can modify video metadata.", HttpStatus.FORBIDDEN);
      return;
    }
    if (modifyVideoMetadataRequest.getCourseId() == null) {
      httpResponseWriter.writeFailedResponse(response, "You need to provide courseId.", HttpStatus.BAD_REQUEST);
      return;
    }
    if (modifyVideoMetadataRequest.getItemId() == null) {
      httpResponseWriter.writeFailedResponse(response, "You need to provide itemId.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, BackendOperationErrors> addResp = videoMetadataService.modifyVideoMetadata(modifyVideoMetadataRequest, UUID.fromString(request.getAttribute("userId").toString()));
    httpResponseWriter.handleDifferentResponses(response, addResp, HttpStatus.CREATED);
  }
}
