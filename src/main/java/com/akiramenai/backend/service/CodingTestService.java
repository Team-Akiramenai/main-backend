package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CodingTestRepo;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.utility.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class CodingTestService {
  private final JsonSerializer jsonSerializer = new JsonSerializer();

  private final CourseRepo courseRepo;
  private final CodingTestRepo codingTestRepo;

  public CodingTestService(CodingTestRepo codingTestRepo, CourseRepo courseRepo) {
    this.codingTestRepo = codingTestRepo;
    this.courseRepo = courseRepo;
  }

  public ResultOrError<String, BackendOperationErrors> addCodingTest(AddCodingTestRequest addCodingTestRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    UUID courseId;
    try {
      courseId = UUID.fromString(addCodingTestRequest.courseId());
    } catch (Exception e) {
      log.error("Failed to parse courseId. Reason: {}", e.getMessage());

      return resp
          .errorMessage("Failed to parse courseId. Invalid courseId provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve requested course.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .result(null)
          .errorMessage("Can't upload coding test. You're not the author of the course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    CodingTest codingTestToAdd = CodingTest
        .builder()
        .courseId(courseId)
        .itemId("CT_" + UUID.randomUUID())
        .question(addCodingTestRequest.question())
        .expectedStdout(addCodingTestRequest.expectedStdout())
        .build();

    try {
      codingTestRepo.save(codingTestToAdd);
      targetCourse.get().getCourseItemIds().add(codingTestToAdd.getItemId());
      courseRepo.save(targetCourse.get());

      ItemId responseObj = new ItemId(codingTestToAdd.getItemId());
      Optional<String> respJson = jsonSerializer.serialize(responseObj);
      if (respJson.isEmpty()) {
        return resp
            .errorMessage("Failed to add the coding test to the course.")
            .errorType(BackendOperationErrors.FailedToSerializeJson)
            .build();
      }

      return resp
          .result(respJson.get())
          .errorMessage(null)
          .errorType(null)
          .build();
    } catch (Exception e) {
      log.error("Error saving coding test. Reason: ", e);

      return resp
          .result(null)
          .errorMessage("Failed to save coding test.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }
  }

  public ResultOrError<String, BackendOperationErrors> deleteCodingTest(DeleteCourseItemRequest deleteCourseItemRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    UUID courseId;
    try {
      courseId = UUID.fromString(deleteCourseItemRequest.courseId());
    } catch (Exception e) {
      log.error("Failed to parse courseId. Reason: {}", e.getMessage());

      return resp
          .errorMessage("Failed to parse courseId. Invalid courseId provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return resp
          .errorMessage("Requested course not found.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .errorMessage("Can't delete the course item. You're not the author of the course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    Optional<CodingTest> retrievedCodingTest = codingTestRepo.findCodingTestByItemId(deleteCourseItemRequest.itemId());
    if (retrievedCodingTest.isEmpty()) {
      return resp
          .errorMessage("Requested coding test not found.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    try {
      targetCourse.get().getCourseItemIds().remove(retrievedCodingTest.get().getItemId());
      courseRepo.save(targetCourse.get());

      codingTestRepo.delete(retrievedCodingTest.get());
    } catch (Exception e) {
      log.error("Failed to delete the coding test requested for removal. Reason: ", e);

      return resp
          .errorMessage("Failed to delete the coding test requested for removal.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemId responseObj = new ItemId(retrievedCodingTest.get().getItemId());
    Optional<String> respJson = jsonSerializer.serialize(responseObj);
    if (respJson.isEmpty()) {
      return resp
          .errorMessage("Failed to serialize JSON.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return resp
        .result(respJson.get())
        .build();
  }

  public ResultOrError<String, BackendOperationErrors> modifyCodingTest(ModifyCodingTestRequest modifyCodingTestRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    UUID courseId;
    try {
      courseId = UUID.fromString(modifyCodingTestRequest.courseId());
    } catch (Exception e) {
      log.error("Failed to parse courseId. Reason: {}", e.getMessage());

      return resp
          .errorMessage("Failed to parse courseId. Invalid courseId provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve requested course.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .result(null)
          .errorMessage("Can't modify the course item. You're not the author of the course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    Optional<CodingTest> codingTestToModify = codingTestRepo.findCodingTestByItemId(modifyCodingTestRequest.itemId());
    if (codingTestToModify.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve the item requested for modification.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    if (modifyCodingTestRequest.question() != null) {
      codingTestToModify.get().setQuestion(modifyCodingTestRequest.question().trim());
    }
    if (modifyCodingTestRequest.expectedStdout() != null) {
      codingTestToModify.get().setExpectedStdout(modifyCodingTestRequest.expectedStdout().trim());
    }

    try {
      codingTestRepo.save(codingTestToModify.get());
    } catch (Exception e) {
      log.error("Failed to modify the coding test. Reason: ", e);

      return resp
          .errorMessage("Failed to modify the coding test.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemId codingTestId = new ItemId(codingTestToModify.get().getItemId());
    Optional<String> responseJson = jsonSerializer.serialize(codingTestId);
    if (responseJson.isEmpty()) {
      return resp
          .errorMessage("Failed to serialize JSON response.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return resp
        .result(responseJson.get())
        .build();
  }
}
