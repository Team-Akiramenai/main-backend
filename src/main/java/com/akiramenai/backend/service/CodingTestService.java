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

  public ResultOrError<String, CourseItemOperationErrors> addCodingTest(AddCodingTestRequest addCodingTestRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, CourseItemOperationErrors>builder();

    Optional<Course> targetCourse = courseRepo.findCourseById(addCodingTestRequest.courseId());
    if (targetCourse.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve requested course.")
          .errorType(CourseItemOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .result(null)
          .errorMessage("Can't upload coding test. You're not the author of the course.")
          .errorType(CourseItemOperationErrors.AttemptingToModifyOthersCourse)
          .build();
    }

    CodingTest codingTestToAdd = CodingTest
        .builder()
        .courseId(addCodingTestRequest.courseId())
        .question(addCodingTestRequest.question())
        .expectedStdout(addCodingTestRequest.expectedStdout())
        .build();

    try {
      codingTestRepo.save(codingTestToAdd);
      targetCourse.get().getCourseItemIds().add(codingTestToAdd.getId());
      courseRepo.save(targetCourse.get());

      CourseItemIdResponse responseObj = new CourseItemIdResponse(codingTestToAdd.getId());
      Optional<String> respJson = jsonSerializer.serialize(responseObj);
      if (respJson.isEmpty()) {
        return resp
            .errorMessage("Failed to add the coding test to the course.")
            .errorType(CourseItemOperationErrors.FailedToSerializeJson)
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
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }
  }


  public ResultOrError<String, CourseItemOperationErrors> deleteCodingTest(DeleteCourseItemRequest deleteCourseItemRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, CourseItemOperationErrors>builder();

    Optional<Course> targetCourse = courseRepo.findCourseById(deleteCourseItemRequest.courseId());
    if (targetCourse.isEmpty()) {
      return resp
          .errorMessage("Requested course not found.")
          .errorType(CourseItemOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .errorMessage("Can't delete the course item. You're not the author of the course.")
          .errorType(CourseItemOperationErrors.AttemptingToModifyOthersCourse)
          .build();
    }

    Optional<CodingTest> retrievedCodingTest = codingTestRepo.findCodingTestById(deleteCourseItemRequest.itemId());
    if (retrievedCodingTest.isEmpty()) {
      return resp
          .errorMessage("Requested coding test not found.")
          .errorType(CourseItemOperationErrors.ItemNotFound)
          .build();
    }

    try {
      targetCourse.get().getCourseItemIds().remove(retrievedCodingTest.get().getId());
      courseRepo.save(targetCourse.get());

      codingTestRepo.delete(retrievedCodingTest.get());
    } catch (Exception e) {
      log.error("Failed to delete the coding test requested for removal. Reason: ", e);

      return resp
          .errorMessage("Failed to delete the coding test requested for removal.")
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }

    CourseItemIdResponse responseObj = new CourseItemIdResponse(retrievedCodingTest.get().getId());
    Optional<String> respJson = jsonSerializer.serialize(responseObj);
    if (respJson.isEmpty()) {
      return resp
          .errorMessage("Failed to serialize JSON.")
          .errorType(CourseItemOperationErrors.FailedToSerializeJson)
          .build();
    }

    return resp
        .result(respJson.get())
        .build();
  }

  public ResultOrError<String, CourseItemOperationErrors> modifyCodingTest(ModifyCodingTestRequest modifyCodingTestRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, CourseItemOperationErrors>builder();

    Optional<Course> targetCourse = courseRepo.findCourseById(UUID.fromString(modifyCodingTestRequest.courseId()));
    if (targetCourse.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve requested course.")
          .errorType(CourseItemOperationErrors.CourseNotFound)
          .build();
    }

    if (targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .result(null)
          .errorMessage("Can't modify the course item. You're not the author of the course.")
          .errorType(CourseItemOperationErrors.AttemptingToModifyOthersCourse)
          .build();
    }

    Optional<CodingTest> codingTestToModify = codingTestRepo.findCodingTestById(UUID.fromString(modifyCodingTestRequest.codingTestId()));
    if (codingTestToModify.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve the item requested for modification.")
          .errorType(CourseItemOperationErrors.ItemNotFound)
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
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }

    Optional<String> responseJson = jsonSerializer.serialize(codingTestToModify.get().getId());
    if (responseJson.isEmpty()) {
      return resp
          .errorMessage("Failed to serialize JSON response.")
          .errorType(CourseItemOperationErrors.FailedToSerializeJson)
          .build();
    }

    return resp
        .result(responseJson.get())
        .build();
  }
}
