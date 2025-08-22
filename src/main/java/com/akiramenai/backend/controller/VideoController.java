package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.service.VideoMetadataService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
  public void uploadVideo(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestBody UploadVideoRequest uploadVideoRequest
  ) {
    if (!request.getAttribute("accountType").equals("Instructor")) {
      httpResponseWriter.writeFailedResponse(response, "Only instructors can upload videos.", HttpStatus.FORBIDDEN);
      return;
    }
    if (uploadVideoRequest.getCourseId() == null) {
      httpResponseWriter.writeFailedResponse(response, "Associated course ID not provided.", HttpStatus.BAD_REQUEST);
      return;
    }
    if (uploadVideoRequest.getVideoMetadataId() == null) {
      httpResponseWriter.writeFailedResponse(response, "VideoMetadataId not provided.", HttpStatus.BAD_REQUEST);
      return;
    }
    if (uploadVideoRequest.getTitle() == null || uploadVideoRequest.getTitle().isBlank()) {
      httpResponseWriter.writeFailedResponse(response, "Video title must not be empty or filled with only whitespace characters.", HttpStatus.BAD_REQUEST);
      return;
    }
    if (uploadVideoRequest.getDescription() == null || uploadVideoRequest.getDescription().isBlank()) {
      httpResponseWriter.writeFailedResponse(response, "Video description must not be empty or filled with only whitespace characters.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, CourseItemOperationErrors> addResp = videoMetadataService.addVideoMetadata(uploadVideoRequest, UUID.fromString(request.getAttribute("userId").toString()));
    httpResponseWriter.handleDifferentResponses(response, addResp, HttpStatus.CREATED);
  }
}
