package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.repo.QuizRepo;
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

  public ResultOrError<String, CourseItemOperationErrors> addQuiz(AddQuizRequest addQuizRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, CourseItemOperationErrors>builder();

    Optional<Course> targetCourse = courseRepo.findCourseById(addQuizRequest.courseId());
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
          .errorMessage("Can't upload quiz. You're not the author of the course.")
          .errorType(CourseItemOperationErrors.AttemptingToModifyOthersCourse)
          .build();
    }

    Quiz quizToAdd = Quiz
        .builder()
        .courseId(addQuizRequest.courseId())
        .question(addQuizRequest.question())
        .option1(addQuizRequest.o1())
        .option2(addQuizRequest.o2())
        .option3(addQuizRequest.o3())
        .option4(addQuizRequest.o4())
        .correctOption(addQuizRequest.correctOption())
        .build();


    try {
      quizRepo.save(quizToAdd);
      targetCourse.get().getCourseItemIds().add(quizToAdd.getId());
      courseRepo.save(targetCourse.get());

      CourseItemIdResponse quizItemId = new CourseItemIdResponse(quizToAdd.getId());
      Optional<String> responseJson = jsonSerializer.serialize(quizItemId);
      if (responseJson.isEmpty()) {
        return resp
            .errorMessage("Failed to serialize JSON response.")
            .errorType(CourseItemOperationErrors.FailedToSerializeJson)
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
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }
  }

  public Optional<String> deleteQuiz(DeleteCourseItemRequest deleteCourseItemRequest, UUID currentUserId) {
    Optional<Course> targetCourse = courseRepo.findCourseById(deleteCourseItemRequest.courseId());
    if (targetCourse.isEmpty()) {
      return Optional.of("Failed to retrieve requested course.");
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return Optional.of("Can't modify the course item. You're not the author of the course.");
    }

    Optional<Quiz> retrievedQuiz = quizRepo.findQuizById(deleteCourseItemRequest.itemId());
    if (retrievedQuiz.isEmpty()) {
      return Optional.of("Failed to retrieve the item requested for removal.");
    }

    try {
      targetCourse.get().getCourseItemIds().remove(retrievedQuiz.get().getId());
      courseRepo.save(targetCourse.get());

      quizRepo.delete(retrievedQuiz.get());
    } catch (Exception e) {
      log.error("Failed to delete the quiz from the course. Reason: ", e);

      return Optional.of("Failed to delete the quiz from the course.");
    }

    return Optional.empty();
  }

  public ResultOrError<String, CourseItemOperationErrors> modifyQuiz(ModifyQuizRequest modifyQuizRequest, UUID currentUserId) {
    var resp = ResultOrError.<String, CourseItemOperationErrors>builder();

    Optional<Course> targetCourse = courseRepo.findCourseById(UUID.fromString(modifyQuizRequest.courseId()));
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

    Optional<Quiz> quizToModify = quizRepo.findQuizById(UUID.fromString(modifyQuizRequest.quizId()));
    if (quizToModify.isEmpty()) {
      return resp
          .result(null)
          .errorMessage("Failed to retrieve the item requested for modification.")
          .errorType(CourseItemOperationErrors.ItemNotFound)
          .build();
    }

    if (modifyQuizRequest.question() != null) {
      quizToModify.get().setQuestion(modifyQuizRequest.question().trim());
    }
    if (modifyQuizRequest.option1() != null) {
      quizToModify.get().setOption1(modifyQuizRequest.option1().trim());
    }
    if (modifyQuizRequest.option2() != null) {
      quizToModify.get().setOption2(modifyQuizRequest.option2().trim());
    }
    if (modifyQuizRequest.option3() != null) {
      quizToModify.get().setOption3(modifyQuizRequest.option3().trim());
    }
    if (modifyQuizRequest.option4() != null) {
      quizToModify.get().setOption4(modifyQuizRequest.option4().trim());
    }
    if (modifyQuizRequest.correctOption() != null) {
      quizToModify.get().setCorrectOption(modifyQuizRequest.correctOption());
    }

    try {
      quizRepo.save(quizToModify.get());

      Optional<String> responseJson = jsonSerializer.serialize(quizToModify.get().getId());
      if (responseJson.isEmpty()) {
        return resp
            .errorMessage("Failed to serialize JSON response.")
            .errorType(CourseItemOperationErrors.FailedToSerializeJson)
            .build();
      }

      return resp
          .result(responseJson.get())
          .build();
    } catch (Exception e) {
      log.error("Failed to save the quiz requested for modification. Reason: ", e);

      return resp
          .errorMessage("Failed to save the modified quiz.")
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }
  }
}
