package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.LearnerInfosRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.UserService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class CourseController {
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final UserService userService;
  private final LearnerInfosRepo learnerInfosRepo;
  private final CourseService courseService;

  public CourseController(CourseService courseService, UserService userService, LearnerInfosRepo learnerInfosRepo) {
    this.courseService = courseService;
    this.userService = userService;
    this.learnerInfosRepo = learnerInfosRepo;
  }

  private Sort.Direction getSortDirection(String sorting) {
    Sort.Direction sortDirection = Sort.Direction.ASC;
    if (sorting.equals("DESC")) {
      sortDirection = Sort.Direction.DESC;
    }

    return sortDirection;
  }

  @GetMapping("api/public/get/course")
  public void getCourse(
      HttpServletResponse httpResponse,
      @RequestParam(required = true) String courseId
  ) {
    Optional<UUID> courseUUID = IdParser.parseId(courseId);
    if (courseUUID.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to parse the provided courseId. Invalid courseId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<Course> targetCourse = courseService.getCourse(courseUUID.get());
    if (targetCourse.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to retrieve the course. Course not found.", HttpStatus.NOT_FOUND);
      return;
    }

    Optional<String> respJson = jsonSerializer.serialize(new CleanedCourse(targetCourse.get()));
    if (respJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize the response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
  }

  @GetMapping("api/public/get/courses")
  public void getCoursesPaginated(
      HttpServletResponse httpResponse,
      @RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "page-size", required = false, defaultValue = "5") int pageSize,
      @RequestParam(value = "sorting", required = false, defaultValue = "ASC") String sorting
  ) {
    PaginatedCourses<CleanedCourse> response = courseService.getAllCoursesPaginated(pageSize, page, getSortDirection(sorting));

    Optional<String> respJson = jsonSerializer.serialize(response);
    if (respJson.isPresent()) {
      httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
      return;
    }

    httpResponseWriter.writeFailedResponse(httpResponse, "Internal server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR);
  }


  @GetMapping("api/protected/get/my-courses")
  public void getMyCoursesPaginated(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "page", required = false, defaultValue = "0") int page,
      @RequestParam(value = "page-size", required = false, defaultValue = "5") int pageSize,
      @RequestParam(value = "sorting", required = false, defaultValue = "ASC") String sorting
  ) {
    String userId = request.getAttribute("userId").toString();
    String accountType = request.getAttribute("accountType").toString();

    Optional<String> respJson;
    if (accountType.equals("Learner")) {
      PaginatedCourses<CleanedCourse> paginatedCourses = courseService.getLearnerCoursesPaginated(userId, pageSize, page, getSortDirection(sorting));
      respJson = jsonSerializer.serialize(paginatedCourses);
    } else {
      PaginatedCourses<CleanedCoursesForInstructors> paginatedCourses = courseService.getInstructorCoursesPaginated(userId, pageSize, page, getSortDirection(sorting));
      respJson = jsonSerializer.serialize(paginatedCourses);
    }

    if (respJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(response, respJson.get(), HttpStatus.OK);
  }

  @PostMapping("api/protected/add/course")
  public void addCourses(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody AddCourseRequest addCourseRequest
  ) {
    ResultOrError<String, BackendOperationErrors> addCourseResp = courseService.addCourse(
        addCourseRequest,
        UUID.fromString(request.getAttribute("userId").toString())
    );

    httpResponseWriter.handleDifferentResponses(response, addCourseResp, HttpStatus.CREATED);
  }

  @PostMapping("api/protected/set/course-item-order")
  public void updateCourseItemOrder(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody ItemOrderUpdateRequest itemOrderUpdateRequest
  ) {
    ResultOrError<String, BackendOperationErrors> result = courseService.updateCourseItemOrder(
        itemOrderUpdateRequest.courseId(),
        itemOrderUpdateRequest.orderOfItemIds(),
        UUID.fromString(request.getAttribute("userId").toString())
    );

    httpResponseWriter.handleDifferentResponses(response, result, HttpStatus.OK);
  }

  @PostMapping("api/protected/publish-course")
  public ResponseEntity<String> publishCourse(
      HttpServletRequest request,
      @RequestBody CoursePublishRequest publishRequest
  ) {
    if (request.getAttribute("userId") == null) {
      return new ResponseEntity<>("User ID is missing from the request", HttpStatus.BAD_REQUEST);
    }
    if (request.getAttribute("accountType") == null || !request.getAttribute("accountType").equals("Instructor")) {
      return new ResponseEntity<>("Invalid account type. Only instructors can publish courses.", HttpStatus.FORBIDDEN);
    }

    if (publishRequest.courseId() == null) {
      return new ResponseEntity<>("Course ID is missing from the request", HttpStatus.BAD_REQUEST);
    }

    Optional<String> response = courseService.publishCourse(UUID.fromString(publishRequest.courseId()), UUID.fromString(request.getAttribute("userId").toString()));
    return response
        .map(s -> ResponseEntity
            .badRequest()
            .contentType(MediaType.TEXT_PLAIN)
            .body(s))
        .orElseGet(() -> ResponseEntity
            .ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body("Course published"));

  }


  @PostMapping("api/protected/rate-course")
  public ResponseEntity<String> rateCourse(
      HttpServletRequest request,
      @RequestBody RateCourseRequest rateCourseRequest
  ) {
    if (request.getAttribute("userId") == null) {
      return new ResponseEntity<>("User ID is missing from the request", HttpStatus.BAD_REQUEST);
    }
    if (request.getAttribute("accountType") == null || !request.getAttribute("accountType").equals("Learner")) {
      return new ResponseEntity<>("Invalid account type. Only learners can rate courses.", HttpStatus.FORBIDDEN);
    }

    Optional<Users> user = userService.findUserById(UUID.fromString(request.getAttribute("userId").toString()));
    if (user.isEmpty()) {
      return new ResponseEntity<>("No user by that ID exists", HttpStatus.NOT_FOUND);
    }

    Optional<LearnerInfos> learnerInfos = learnerInfosRepo.findById(user.get().getLearnerForeignKey());
    if (learnerInfos.isEmpty()) {
      return new ResponseEntity<>("Learner's associated information was not found", HttpStatus.NOT_FOUND);
    }

    // only learners who've purchased the course can rate it
    List<UUID> purchasedCourses = learnerInfos.get().getMyPurchasedCourses();
    boolean isCoursePurchased = purchasedCourses.contains(UUID.fromString(rateCourseRequest.courseId()));
    if (!isCoursePurchased) {
      return new ResponseEntity<>("Learner can't rate a course that they haven't purchased", HttpStatus.FORBIDDEN);
    }

    Optional<String> response = courseService.addRating(UUID.fromString(rateCourseRequest.courseId()), rateCourseRequest.rating());
    return response
        .map(
            errorReason -> ResponseEntity
                .badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(errorReason)
        )
        .orElseGet(
            () -> ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("Course rated successfully")
        );
  }
}
