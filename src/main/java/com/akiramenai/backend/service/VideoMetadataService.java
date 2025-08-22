package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.repo.VideoMetadataRepo;
import com.akiramenai.backend.utility.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class VideoMetadataService {
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final VideoMetadataRepo videoMetadataRepo;
  private final CourseRepo courseRepo;

  public VideoMetadataService(
      VideoMetadataRepo videoMetadataRepo,
      CourseRepo courseRepo
  ) {
    this.videoMetadataRepo = videoMetadataRepo;
    this.courseRepo = courseRepo;
  }

  public ResultOrError<String, CourseItemOperationErrors> addVideoMetadata(UploadVideoRequest uploadVideoRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, CourseItemOperationErrors>builder();

    Optional<Course> targetCourse = courseRepo.findCourseById(
        UUID.fromString(uploadVideoRequest.getCourseId())
    );
    if (targetCourse.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to find the requested course.")
          .errorType(CourseItemOperationErrors.CourseNotFound)
          .build();
    }

    if (!currentUserId.equals(targetCourse.get().getInstructorId())) {
      return resp
          .result(null)
          .errorMessage("Can't upload video. You're not the author of the course.")
          .errorType(CourseItemOperationErrors.AttemptingToModifyOthersCourse)
          .build();
    }

    Optional<VideoMetadata> targetVideoMetadata = videoMetadataRepo.findVideoMetadataById(
        UUID.fromString(uploadVideoRequest.getVideoMetadataId())
    );
    if (targetVideoMetadata.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to find the requested video metadata.")
          .errorType(CourseItemOperationErrors.ItemNotFound)
          .build();
    }

    targetVideoMetadata.get().setTitle(uploadVideoRequest.getTitle());
    targetVideoMetadata.get().setDescription(uploadVideoRequest.getDescription());
    targetVideoMetadata.get().setLastModifiedDateTime(LocalDateTime.now());

    try {
      videoMetadataRepo.save(targetVideoMetadata.get());

      CourseItemIdResponse responseObj = new CourseItemIdResponse(targetCourse.get().getId());
      Optional<String> respJson = jsonSerializer.serialize(responseObj);
      if (respJson.isEmpty()) {
        return resp
            .errorMessage("Failed to serialize video metadata.")
            .errorType(CourseItemOperationErrors.FailedToSerializeJson)
            .build();
      }

      return resp
          .result(respJson.get())
          .errorMessage(null)
          .errorType(null)
          .build();
    } catch (Exception e) {
      log.error("Error saving video metadata. Reason: ", e);

      return resp
          .result(null)
          .errorMessage("Failed to save video metadata.")
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }
  }
}
