package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.repo.VideoMetadataRepo;
import com.akiramenai.backend.utility.IdParser;
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

  public ResultOrError<String, BackendOperationErrors> modifyVideoMetadata(ModifyVideoMetadataRequest modifyVideoMetadataRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<UUID> courseId = IdParser.parseId(modifyVideoMetadataRequest.getCourseId());
    if (courseId.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to parse the provided courseId. Invalid courseId provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(
        courseId.get()
    );
    if (targetCourse.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to find the requested course.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    if (!currentUserId.equals(targetCourse.get().getInstructorId())) {
      return resp
          .result(null)
          .errorMessage("Can't upload video. You're not the author of the course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    Optional<VideoMetadata> targetVideoMetadata = videoMetadataRepo.findVideoMetadataById(
        UUID.fromString(modifyVideoMetadataRequest.getItemId())
    );
    if (targetVideoMetadata.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to find the requested video metadata.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    targetVideoMetadata.get().setTitle(modifyVideoMetadataRequest.getTitle());
    targetVideoMetadata.get().setDescription(modifyVideoMetadataRequest.getDescription());
    targetVideoMetadata.get().setLastModifiedDateTime(LocalDateTime.now());

    try {
      videoMetadataRepo.save(targetVideoMetadata.get());

      ItemId responseObj = new ItemId(targetVideoMetadata.get().getItemId());
      Optional<String> respJson = jsonSerializer.serialize(responseObj);
      if (respJson.isEmpty()) {
        return resp
            .errorMessage("Failed to serialize video metadata.")
            .errorType(BackendOperationErrors.FailedToSerializeJson)
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
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }
  }
}
