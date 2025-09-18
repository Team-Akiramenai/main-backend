package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.LearnerInfosRepo;
import com.akiramenai.backend.repo.PurchaseRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.StorageService;
import com.akiramenai.backend.service.UserService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class CourseController {
  private final StorageService storageService;
  private final PurchaseRepo purchaseRepo;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  @Value("${application.default-values.media.picture-directory}")
  private String pictureDirectory;

  @Value("${application.default-values.default-course-thumbnail-filename}")
  private String defaultCourseThumbnailFilename;

  private final UserService userService;
  private final LearnerInfosRepo learnerInfosRepo;
  private final CourseService courseService;

  public CourseController(CourseService courseService, UserService userService, LearnerInfosRepo learnerInfosRepo, StorageService storageService, PurchaseRepo purchaseRepo) {
    this.courseService = courseService;
    this.userService = userService;
    this.learnerInfosRepo = learnerInfosRepo;
    this.storageService = storageService;
    this.purchaseRepo = purchaseRepo;
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

    Optional<Long> coursePurchaseCount = purchaseRepo.countByCourseId(courseUUID.get());
    if (coursePurchaseCount.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed retrieve purchase info.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    Optional<String> respJson = jsonSerializer.serialize(new DetailedCourse(targetCourse.get(), coursePurchaseCount.get()));
    if (respJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize the response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
  }

  @GetMapping("api/public/get/course-thumbnail/{course-id}")
  public ResponseEntity<InputStreamResource> getCourseThumbnail(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @PathVariable(name = "course-id") String courseId
  ) {
    Optional<UUID> courseUUID = IdParser.parseId(courseId);
    if (courseUUID.isEmpty()) {
      return ResponseEntity.internalServerError().build();
    }
    Optional<Course> targetCourse = courseService.getCourse(courseUUID.get());
    if (targetCourse.isEmpty()) {
      return ResponseEntity.notFound().build();
    }


    InputStream is = null;
    MediaType imageType = null;
    try {
      // if the course has no thumbnail, return the default thumbnail. Otherwise, return the
      // course's thumbnail
      if (targetCourse.get().getThumbnailImageName() == null) {
        Path defaultPicturePath = Paths.get(pictureDirectory, defaultCourseThumbnailFilename);
        is = new FileInputStream(defaultPicturePath.toFile());
        imageType = StorageService.getFileType(defaultPicturePath);
      } else {
        Path pfpFilePath = Paths.get(pictureDirectory, targetCourse.get().getThumbnailImageName());
        is = new FileInputStream(pfpFilePath.toFile());
        imageType = StorageService.getFileType(pfpFilePath);
      }
    } catch (Exception e) {
      log.error("Failed to get the thumbnail of the course. Reason: {}", e.getMessage());

      return ResponseEntity.internalServerError().build();
    }

    if (!imageType.equals(MediaType.IMAGE_JPEG) && !imageType.equals(MediaType.IMAGE_PNG)) {
      return ResponseEntity.internalServerError().build();
    }

    return ResponseEntity
        .ok()
        .contentType(imageType)
        .body(new InputStreamResource(is));
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

  @PostMapping("api/protected/modify/course")
  public void modifyCourse(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody CourseModificationRequest courseModificationRequest
  ) {
    Optional<BackendOperationErrors> modifyOpErr = courseService.updateCourse(
        courseModificationRequest,
        UUID.fromString(request.getAttribute("userId").toString())
    );

    if (modifyOpErr.isEmpty()) {
      ItemId courseId = new ItemId(courseModificationRequest.getCourseId().toString());
      Optional<String> respJson = jsonSerializer.serialize(courseId);
      if (respJson.isEmpty()) {
        httpResponseWriter.writeFailedResponse(response, "Failed to serialize the response JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
        return;
      }
      httpResponseWriter.writeOkResponse(response, respJson.get(), HttpStatus.OK);
      return;
    }

    switch (modifyOpErr.get()) {
      case CourseNotFound ->
          httpResponseWriter.writeFailedResponse(response, "Course not found.", HttpStatus.NOT_FOUND);
      case AttemptingToModifyOthersItem ->
          httpResponseWriter.writeFailedResponse(response, "Attempting to modify other user's items.", HttpStatus.BAD_REQUEST);
      case InvalidRequest ->
          httpResponseWriter.writeFailedResponse(response, "Invalid request sent. Please make sure the request follows the guidelines.", HttpStatus.BAD_REQUEST);
      default -> httpResponseWriter.writeFailedResponse(response, "Internal error.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("api/protected/change-course-thumbnail")
  public void changeCourseThumbnail(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam(name = "course-id") String courseId,
      @RequestParam("new-thumbnail") MultipartFile newThumbnail
  ) {
    if (newThumbnail.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Provided file is empty.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, FileUploadErrorTypes> savedThumbnail = storageService.saveImage(newThumbnail);
    if (savedThumbnail.errorType() != null) {
      switch (savedThumbnail.errorType()) {
        case UnsupportedFileType, FileIsEmpty ->
            httpResponseWriter.writeFailedResponse(response, savedThumbnail.errorMessage(), HttpStatus.BAD_REQUEST);
        case InvalidUploadDir, FailedToCreateUploadDir, FailedToSaveFile ->
            httpResponseWriter.writeFailedResponse(response, savedThumbnail.errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    Optional<UUID> userId = IdParser.parseId(request.getAttribute("userId").toString());
    if (userId.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Invalid userId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<UUID> courseUUID = IdParser.parseId(courseId);
    if (courseUUID.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Invalid course ID provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<Course> targetCourse = courseService.getCourse(courseUUID.get());
    if (targetCourse.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Course not found.", HttpStatus.NOT_FOUND);
      return;
    }

    ResultOrError<Boolean, BackendOperationErrors> res = courseService.updateCourseThumbnail(
        courseUUID.get(),
        userId.get(),
        savedThumbnail.result()
    );
    if (res.errorType() != null) {
      switch (res.errorType()) {
        case CourseNotFound ->
            httpResponseWriter.writeFailedResponse(response, res.errorMessage(), HttpStatus.NOT_FOUND);
        case AttemptingToModifyOthersItem ->
            httpResponseWriter.writeFailedResponse(response, res.errorMessage(), HttpStatus.BAD_REQUEST);
      }
    }

    httpResponseWriter.writeIdResponse(response, targetCourse.get().getId().toString(), HttpStatus.OK);
  }

  @PostMapping("api/protected/remove/course")
  public void removeCourse(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody CourseDeletionRequest courseDeletionRequest
  ) {
    Optional<UUID> courseId = IdParser.parseId(courseDeletionRequest.courseId());
    if (courseId.isEmpty()) {
      httpResponseWriter.writeFailedResponse(response, "Invalid item id.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, BackendOperationErrors> deleteOpErr = courseService.deleteCourse(
        courseId.get(),
        UUID.fromString(request.getAttribute("userId").toString())
    );
    httpResponseWriter.handleDifferentResponses(response, deleteOpErr, HttpStatus.OK);
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

  @PostMapping("api/protected/add/tags")
  public void addTags(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestBody AddTagsRequest addTagsRequest
  ) {
    UUID userId = UUID.fromString(httpRequest.getAttribute("userId").toString());

    Optional<UUID> courseId = IdParser.parseId(addTagsRequest.courseId());
    if (courseId.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Invalid courseId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, BackendOperationErrors> res = courseService.addTags(
        userId,
        courseId.get(),
        addTagsRequest.tagsToBeAdded()
    );
    httpResponseWriter.handleDifferentResponses(httpResponse, res, HttpStatus.CREATED);
  }


  @PostMapping("api/protected/modify/tags")
  public void modifyTags(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestBody ModifyTagsRequest modifyTagsRequest
  ) {
    UUID userId = UUID.fromString(httpRequest.getAttribute("userId").toString());

    Optional<UUID> courseId = IdParser.parseId(modifyTagsRequest.courseId());
    if (courseId.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Invalid courseId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, BackendOperationErrors> res = courseService.modifyTagsList(
        userId,
        courseId.get(),
        modifyTagsRequest.modifiedTagList()
    );
    httpResponseWriter.handleDifferentResponses(httpResponse, res, HttpStatus.OK);
  }


  @PostMapping("api/protected/delete/tags")
  public void deleteTags(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,

      @RequestBody DeleteTagsRequest deleteTagsRequest
  ) {
    UUID userId = UUID.fromString(httpRequest.getAttribute("userId").toString());

    Optional<UUID> courseId = IdParser.parseId(deleteTagsRequest.courseId());
    if (courseId.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Invalid courseId provided.", HttpStatus.BAD_REQUEST);
      return;
    }

    ResultOrError<String, BackendOperationErrors> res = courseService.deleteTags(
        userId,
        courseId.get(),
        deleteTagsRequest.tagsToBeDeleted()
    );
    httpResponseWriter.handleDifferentResponses(httpResponse, res, HttpStatus.OK);
  }
}
