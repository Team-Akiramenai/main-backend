package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.repo.VideoMetadataRepo;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.aspectj.util.FileUtil;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class VideoMetadataService {
  private final UserService userService;
  private final StorageService storageService;
  private final UserRepo userRepo;
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final VideoMetadataRepo videoMetadataRepo;
  private final CourseRepo courseRepo;

  public VideoMetadataService(
      VideoMetadataRepo videoMetadataRepo,
      CourseRepo courseRepo,
      UserService userService, StorageService storageService, UserRepo userRepo) {
    this.videoMetadataRepo = videoMetadataRepo;
    this.courseRepo = courseRepo;
    this.userService = userService;
    this.storageService = storageService;
    this.userRepo = userRepo;
  }

  public ResultOrError<String, BackendOperationErrors> modifyVideoMetadata(
      ModifyVideoMetadataRequest modifyVideoMetadataRequest,
      UUID currentUserId
  ) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<UUID> courseId = IdParser.parseId(modifyVideoMetadataRequest.getCourseId());
    if (courseId.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to parse the provided courseId. Invalid courseId provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<ParsedItemInfo> videoMetadataItem = IdParser.parseItemId(modifyVideoMetadataRequest.getItemId());
    if (videoMetadataItem.isEmpty() || videoMetadataItem.get().itemType() != CourseItems.VideoMetadata) {
      return resp
          .result(null)
          .errorMessage("Invalid itemType provided.")
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
          .errorMessage("Can't modify video metadata. You're not the author of the course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    Optional<VideoMetadata> targetVideoMetadata = videoMetadataRepo.findVideoMetadataByItemId(
        modifyVideoMetadataRequest.getItemId()
    );
    if (targetVideoMetadata.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to find the requested video metadata.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    if (modifyVideoMetadataRequest.getTitle() != null) {
      targetVideoMetadata.get().setTitle(modifyVideoMetadataRequest.getTitle());
    }
    if (modifyVideoMetadataRequest.getDescription() != null) {
      targetVideoMetadata.get().setDescription(modifyVideoMetadataRequest.getDescription());
    }
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

  public ResultOrError<String, BackendOperationErrors> deleteVideoMetadata(
      UUID currentUserId,
      UUID courseId,
      String itemId
  ) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<Users> targetUser = userService.findUserById(currentUserId);
    if (targetUser.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.ItemNotFound)
          .errorMessage("User not found.")
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.CourseNotFound)
          .errorMessage("Course not found.")
          .build();
    }

    Optional<VideoMetadata> videoMetadata = videoMetadataRepo.findVideoMetadataByItemId(itemId);
    if (videoMetadata.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.ItemNotFound)
          .errorMessage("VideoMetadata not found.")
          .build();
    }

    if (!targetCourse.get().getId().equals(videoMetadata.get().getCourseId())) {
      return resp
          .errorType(BackendOperationErrors.InvalidRequest)
          .errorMessage("The item is not part of the target course.")
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .errorMessage("Can't modify video metadata. You're not the author of the course.")
          .build();
    }

    // get size of itemId dir and then delete
    try {
      String videoItemIdWithoutPrefix = itemId.substring(3);
      Path videoIdDirPath = Paths.get(
          storageService.videoDirectoryString,
          videoItemIdWithoutPrefix
      );

      long contentSize = FileUtils.sizeOf(videoIdDirPath.toFile());

      FileUtils.deleteDirectory(videoIdDirPath.toFile());

      targetUser.get().setUsedStorageInBytes(targetUser.get().getUsedStorageInBytes() - contentSize);
      userRepo.save(targetUser.get());

    } catch (Exception e) {
      log.error("Error getting VideoItemId path. Reason: ", e);

      return resp
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .errorMessage("Failed to get VideoItemId path.")
          .build();
    }

    ItemId responseObj = new ItemId(itemId);
    Optional<String> respJson = jsonSerializer.serialize(responseObj);
    if (respJson.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .errorMessage("Failed to serialize JSON response.")
          .build();
    }

    return resp
        .result(respJson.get())
        .build();
  }
}
