package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.repo.PurchaseRepo;
import com.akiramenai.backend.repo.TerminalTestRepo;
import com.akiramenai.backend.utility.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class TerminalTestService {
  private final JsonSerializer jsonSerializer = new JsonSerializer();


  private final CourseRepo courseRepo;
  private final TerminalTestRepo terminalTestRepo;
  private final StorageService storageService;
  private final PurchaseRepo purchaseRepo;

  public TerminalTestService(TerminalTestRepo terminalTestRepo, CourseRepo courseRepo, StorageService storageService, PurchaseRepo purchaseRepo) {
    this.terminalTestRepo = terminalTestRepo;
    this.courseRepo = courseRepo;
    this.storageService = storageService;
    this.purchaseRepo = purchaseRepo;
  }

  public ResultOrError<String, BackendOperationErrors> addTerminalTest(
      AddTerminalTestRequest addTerminalTestRequest,
      UUID currentUserId
  ) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    UUID courseId;
    try {
      courseId = UUID.fromString(addTerminalTestRequest.courseId());
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
          .errorMessage("Failed to add terminal test. You're not the author of the course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    TerminalTest terminalTest = TerminalTest
        .builder()
        .courseId(courseId)
        .itemId("TT_" + UUID.randomUUID())
        .question(addTerminalTestRequest.question())
        .description(addTerminalTestRequest.description())
        .verificationScriptFilename(addTerminalTestRequest.evalScript().getFileName().toString())
        .build();

    try {
      terminalTestRepo.save(terminalTest);
      targetCourse.get().getCourseItemIds().add(terminalTest.getItemId());
      courseRepo.save(targetCourse.get());

      ItemId responseObj = new ItemId(terminalTest.getItemId());
      Optional<String> respJson = jsonSerializer.serialize(responseObj);
      if (respJson.isEmpty()) {
        return resp
            .errorMessage("Failed to add the terminal test to the course.")
            .errorType(BackendOperationErrors.FailedToSerializeJson)
            .build();
      }

      return resp
          .result(respJson.get())
          .errorMessage(null)
          .errorType(null)
          .build();
    } catch (Exception e) {
      log.error("Error saving terminal test. Reason: ", e);

      return resp
          .result(null)
          .errorMessage("Failed to save terminal test.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }
  }

  public ResultOrError<String, BackendOperationErrors> deleteTerminalTest(
      DeleteCourseItemRequest deleteCourseItemRequest,
      UUID currentUserId
  ) {
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

    Optional<TerminalTest> retrievedTerminalTest = terminalTestRepo.findTerminalTestByItemId(deleteCourseItemRequest.itemId());
    if (retrievedTerminalTest.isEmpty()) {
      return resp
          .errorMessage("Requested terminal test not found.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    try {
      targetCourse.get().getCourseItemIds().remove(retrievedTerminalTest.get().getItemId());
      courseRepo.save(targetCourse.get());

      terminalTestRepo.delete(retrievedTerminalTest.get());

      Path scriptFileToRemove = Paths.get(
          storageService.scriptDirectoryString,
          retrievedTerminalTest.get().getVerificationScriptFilename()
      );
      if (!scriptFileToRemove.toFile().delete()) {
        log.warn("Failed to remove deleted script file: {}", scriptFileToRemove);
      }
    } catch (Exception e) {
      log.error("Failed to delete the terminal test requested for removal. Reason: ", e);

      return resp
          .errorMessage("Failed to delete the terminal test requested for removal.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemId responseObj = new ItemId(retrievedTerminalTest.get().getItemId());
    Optional<String> respJson = jsonSerializer.serialize(responseObj);
    if (respJson.isEmpty()) {
      return resp
          .errorMessage("Failed to serialize JSON response.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return resp
        .result(respJson.get())
        .build();
  }

  public ResultOrError<String, BackendOperationErrors> modifyTerminalTest(
      ModifyTerminalTestRequest modifyCodingTestRequest,
      UUID currentUserId
  ) {
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

    Optional<TerminalTest> terminalTestToModify = terminalTestRepo.findTerminalTestByItemId(
        modifyCodingTestRequest.itemId()
    );
    if (terminalTestToModify.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve the item requested for modification.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    if (modifyCodingTestRequest.question() != null) {
      terminalTestToModify.get().setQuestion(modifyCodingTestRequest.question().trim());
    }
    if (modifyCodingTestRequest.description() != null) {
      terminalTestToModify.get().setDescription(modifyCodingTestRequest.description().trim());
    }
    if (modifyCodingTestRequest.newScript() != null) {
      // replace the old script with the new script
      Path oldScriptPath = Paths.get(
          storageService.scriptDirectoryString,
          terminalTestToModify.get().getVerificationScriptFilename()
      );
      try {
        Files.move(modifyCodingTestRequest.newScript(), oldScriptPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        return resp
            .result(null)
            .errorMessage("Failed to save the new script file.")
            .errorType(BackendOperationErrors.ItemNotFound)
            .build();
      }
    }

    try {
      terminalTestRepo.save(terminalTestToModify.get());
    } catch (Exception e) {
      log.error("Failed to modify the terminal test. Reason: ", e);

      return resp
          .errorMessage("Failed to modify the terminal test.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemId terminalTest = new ItemId(terminalTestToModify.get().getItemId());
    Optional<String> responseJson = jsonSerializer.serialize(terminalTest);
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

  public ResultOrError<Path, BackendOperationErrors> getEvalScript(
      String itemId
  ) {
    var resp = ResultOrError.<Path, BackendOperationErrors>builder();

    Optional<TerminalTest> targetTerminalTest = terminalTestRepo.findTerminalTestByItemId(itemId);
    if (targetTerminalTest.isEmpty()) {
      return resp
          .errorType(BackendOperationErrors.ItemNotFound)
          .errorMessage("Terminal Test metadata not found.")
          .build();
    }

    try {
      Path pathToScript = Paths.get(
          storageService.scriptDirectoryString,
          targetTerminalTest.get().getVerificationScriptFilename()
      );
      return resp
          .result(pathToScript)
          .build();
    } catch (Exception e) {
      return resp
          .errorType(BackendOperationErrors.ItemNotFound)
          .errorMessage("Failed to retrieve the script file.")
          .build();
    }

  }
}
