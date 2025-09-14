package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.repo.QuizRepo;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class QuizService {
  JsonSerializer jsonSerializer = new JsonSerializer();
  private final QuizRepo quizRepo;
  private final CourseRepo courseRepo;

  public QuizService(QuizRepo quizRepo, CourseRepo courseRepo) {
    this.quizRepo = quizRepo;
    this.courseRepo = courseRepo;
  }

  public ResultOrError<String, BackendOperationErrors> addQuiz(AddQuizRequest addQuizRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    UUID courseId;
    try {
      courseId = UUID.fromString(addQuizRequest.courseId());
    } catch (Exception e) {
      log.error("Failed to parse courseId. Reason: {}", e.getMessage());

      return resp
          .result(null)
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
          .errorMessage("Can't upload quiz. You're not the author of the course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    Quiz quizToAdd = Quiz
        .builder()
        .courseId(courseId)
        .itemId("QZ_" + UUID.randomUUID())
        .question(addQuizRequest.question())
        .o1(addQuizRequest.o1())
        .o2(addQuizRequest.o2())
        .o3(addQuizRequest.o3())
        .o4(addQuizRequest.o4())
        .correctOption(addQuizRequest.correctOption())
        .build();

    try {
      quizRepo.save(quizToAdd);
      targetCourse.get().getCourseItemIds().add(quizToAdd.getItemId());
      courseRepo.save(targetCourse.get());

      ItemId quizItemId = new ItemId(quizToAdd.getItemId());
      Optional<String> responseJson = jsonSerializer.serialize(quizItemId);
      if (responseJson.isEmpty()) {
        return resp
            .errorMessage("Failed to serialize JSON response.")
            .errorType(BackendOperationErrors.FailedToSerializeJson)
            .build();
      }

      return resp
          .result(responseJson.get())
          .errorMessage(null)
          .errorType(null)
          .build();
    } catch (Exception e) {
      log.error("Error adding the quiz to the course. Reason: ", e);

      return resp
          .result(null)
          .errorMessage("Failed to add the quiz to the course.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }
  }

  public ResultOrError<String, BackendOperationErrors> removeQuiz(DeleteCourseItemRequest deleteCourseItemRequest,
                                                                  UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    UUID courseId;
    try {
      courseId = UUID.fromString(deleteCourseItemRequest.courseId());
    } catch (Exception e) {
      log.error("Failed to parse courseId. Reason: {}", e.getMessage());

      return resp
          .result(null)
          .errorMessage("Failed to parse provided courseId. Invalid courseId provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve requested course. Course not found.")
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

    Optional<Quiz> retrievedQuiz = quizRepo.findQuizByItemId(deleteCourseItemRequest.itemId());
    if (retrievedQuiz.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve the item requested for removal.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    try {
      targetCourse.get().getCourseItemIds().remove(retrievedQuiz.get().getItemId());
      courseRepo.save(targetCourse.get());

      quizRepo.delete(retrievedQuiz.get());
    } catch (Exception e) {
      log.error("Failed to delete the quiz from the course. Reason: ", e);

      return resp
          .result(null)
          .errorMessage("Failed to delete the quiz from the course.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemId quizItemId = new ItemId(retrievedQuiz.get().getItemId());
    Optional<String> responseJson = jsonSerializer.serialize(quizItemId);
    if (responseJson.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to serialize JSON response.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return resp
        .result(responseJson.get())
        .build();
  }

  public ResultOrError<String, BackendOperationErrors> modifyQuiz(ModifyQuizRequest modifyQuizRequest,
                                                                  UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<ParsedItemInfo> quizInfo = IdParser.parseItemId(modifyQuizRequest.itemId());
    if (quizInfo.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to parse itemUUID. Invalid itemUUID provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findCourseById(UUID.fromString(modifyQuizRequest.courseId()));
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

    Optional<Quiz> quizToModify = quizRepo.findQuizByItemId(modifyQuizRequest.itemId());
    if (quizToModify.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve the item requested for modification.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    if (modifyQuizRequest.question() != null) {
      quizToModify.get().setQuestion(modifyQuizRequest.question().trim());
    }
    if (modifyQuizRequest.o1() != null) {
      quizToModify.get().setO1(modifyQuizRequest.o1().trim());
    }
    if (modifyQuizRequest.o2() != null) {
      quizToModify.get().setO2(modifyQuizRequest.o2().trim());
    }
    if (modifyQuizRequest.o3() != null) {
      quizToModify.get().setO3(modifyQuizRequest.o3().trim());
    }
    if (modifyQuizRequest.o4() != null) {
      quizToModify.get().setO4(modifyQuizRequest.o4().trim());
    }
    if (modifyQuizRequest.correctOption() != null) {
      quizToModify.get().setCorrectOption(modifyQuizRequest.correctOption());
    }

    try {
      quizRepo.save(quizToModify.get());

      ItemId quizItemId = new ItemId(quizToModify.get().getItemId());
      Optional<String> responseJson = jsonSerializer.serialize(quizItemId);
      if (responseJson.isEmpty()) {
        return resp
            .errorMessage("Failed to serialize JSON response.")
            .errorType(BackendOperationErrors.FailedToSerializeJson)
            .build();
      }

      return resp
          .result(responseJson.get())
          .build();
    } catch (Exception e) {
      log.error("Failed to save the quiz requested for modification. Reason: ", e);

      return resp
          .errorMessage("Failed to save the modified quiz.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }
  }
}
