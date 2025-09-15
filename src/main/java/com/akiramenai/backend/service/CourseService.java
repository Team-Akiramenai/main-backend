package com.akiramenai.backend.service;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.repo.LearnerInfosRepo;
import com.akiramenai.backend.repo.PurchaseRepo;
import com.akiramenai.backend.utility.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class CourseService {
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final LearnerInfosRepo learnerInfosRepo;
  private final UserService userService;
  private final MeiliService meiliService;
  private final CourseRepo courseRepo;
  private final PurchaseRepo purchaseRepo;
  private final InstructorInfosService instructorInfosService;

  public CourseService(
      CourseRepo courseRepo,
      PurchaseRepo purchaseRepo,
      InstructorInfosService instructorInfosService,
      LearnerInfosRepo learnerInfosRepo,
      UserService userService,
      MeiliService meiliService
  ) {
    this.courseRepo = courseRepo;
    this.purchaseRepo = purchaseRepo;
    this.instructorInfosService = instructorInfosService;
    this.learnerInfosRepo = learnerInfosRepo;
    this.userService = userService;
    this.meiliService = meiliService;
  }

  private Optional<String> basicCourseModificationRequestValidation(CourseModificationRequest courseModificationRequest) {
    if (courseModificationRequest.getCourseId() == null) {
      return Optional.of("Course ID must be provided to modify a course");
    }

    if (courseModificationRequest.getTitle() != null) {
      courseModificationRequest.setTitle(courseModificationRequest.getTitle().trim());
      if (courseModificationRequest.getTitle().length() > 200) {
        return Optional.of("Course title can't have more than 200 characters");
      }
    }
    if (courseModificationRequest.getDescription() != null) {
      courseModificationRequest.setDescription(courseModificationRequest.getDescription().trim());
      if (courseModificationRequest.getDescription().length() > 2000) {
        return Optional.of("Course description can't have more than 2000 characters");
      }
    }

    if (courseModificationRequest.getPrice() != null) {
      if (courseModificationRequest.getPrice() < 1.0) {
        return Optional.of("A course must have a minimum price of 1 dollar (USD)");
      }
    }

    return Optional.empty();
  }

  public ResultOrError<String, BackendOperationErrors> addCourse(AddCourseRequest addCourseRequest, UUID currentUserId) {
    var result = ResultOrError.<String, BackendOperationErrors>builder();

    if (addCourseRequest.title() == null || addCourseRequest.title().isBlank()) {
      return result
          .errorMessage("Title can't be blank.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }
    if (addCourseRequest.description() == null || addCourseRequest.description().isBlank()) {
      return result
          .errorMessage("Description can't be blank.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }
    if (addCourseRequest.price() <= 1.0) {
      return result
          .errorMessage("Price has to be greater than 1.0 USD.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Course courseToAdd = Course
        .builder()
        .instructorId(currentUserId)
        .title(addCourseRequest.title().trim())
        .description(addCourseRequest.description().trim())
        .thumbnailImageName(null)
        .tags(new ArrayList<>())
        .courseItemIds(new ArrayList<>())
        .price(addCourseRequest.price())
        .totalStars(0L)
        .usersWhoRatedCount(0L)
        .createdAt(LocalDateTime.now())
        .isPublished(false)
        .build();

    try {
      courseRepo.save(courseToAdd);
    } catch (Exception e) {
      log.error("Error saving course. Reason: ", e);

      return result
          .errorMessage("Failed to save course.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemId itemId = new ItemId(courseToAdd.getId().toString());
    Optional<String> respJson = jsonSerializer.serialize(itemId);
    if (respJson.isEmpty()) {
      return result
          .errorMessage("Failed to serialize response JSON.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return result
        .result(respJson.get())
        .build();
  }

  public Optional<String> purchaseCourse(String courseId, String buyerId, LocalDateTime purchaseDateTime) {
    if (courseId == null || courseId.isEmpty()) {
      return Optional.of("Course ID is missing from the request.");
    }
    if (buyerId == null || buyerId.isEmpty()) {
      return Optional.of("Buyer ID is missing from the request.");
    }

    Optional<Course> course = courseRepo.findById(UUID.fromString(courseId));
    if (course.isEmpty()) {
      return Optional.of("No course with that ID exists.");
    }
    Course targetCourse = course.get();

    Optional<Users> user = userService.findUserById(UUID.fromString(buyerId));
    if (user.isEmpty()) {
      return Optional.of("No user by that ID exists");
    }
    Optional<LearnerInfos> learnerInfos = learnerInfosRepo.findById(user.get().getLearnerForeignKey());
    if (learnerInfos.isEmpty()) {
      return Optional.of("Learner's associated information was not found");
    }

    // check whether the learner has already purchased this course
    boolean isAlreadyPurchased = learnerInfos.get().getMyPurchasedCourses().contains(targetCourse.getId());
    if (isAlreadyPurchased) {
      return Optional.of("Learner has already purchased this course.");
    }

    learnerInfos.get().getMyPurchasedCourses().add(targetCourse.getId());
    Purchase purchase = Purchase
        .builder()
        .courseId(UUID.fromString(courseId))
        .authorId(targetCourse.getInstructorId())
        .buyerId(UUID.fromString(buyerId))
        .transactionId(UUID.randomUUID())
        .price(targetCourse.getPrice())
        .purchaseTimestamp(purchaseDateTime)
        .purchaseDate(purchaseDateTime.toLocalDate())
        .build();

    try {
      learnerInfosRepo.save(learnerInfos.get());
      purchaseRepo.save(purchase);

      Optional<String> resp = instructorInfosService.courseSold(targetCourse.getInstructorId().toString(), targetCourse.getPrice());
      if (resp.isPresent()) {
        throw new IllegalStateException("Failed to update instructor infos for instructor with id: " + targetCourse.getInstructorId() + " and reason: " + resp.get());
      }

      return Optional.empty();
    } catch (Exception e) {
      log.error("Failed to save purchase for course with id: {}. Reason: {}.", courseId, e.toString());

      return Optional.of("Failed to purchase course.");
    }
  }

  public Optional<String> purchaseCourse(String courseId, String buyerId) {
    return purchaseCourse(courseId, buyerId, LocalDateTime.now());
  }

  public ResultOrError<String, BackendOperationErrors> updateCourseItemOrder(
      String courseId,
      List<String> orderOfItemIds,
      UUID currentUserId
  ) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();

    if (courseId == null) {
      return res
          .errorMessage("No course ID provided.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }
    if (orderOfItemIds == null) {
      return res
          .errorMessage("Did not provide order of item IDs.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    UUID courseUUID;
    try {
      courseUUID = UUID.fromString(courseId);
    } catch (Exception e) {
      log.error("Invalid UUID provided for course ID. Reason: {}", e.toString());

      return res
          .errorMessage("Provided course ID is an invalid UUID.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findById(courseUUID);
    if (targetCourse.isEmpty()) {
      return res
          .errorMessage("No course with that ID exists.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return res
          .errorMessage("Failed to update the ordering of course items. You're not the author of this course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    if (targetCourse.get().getCourseItemIds().size() != orderOfItemIds.size()) {
      return res
          .errorMessage("The number of course item IDs don't match.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    HashMap<String, Boolean> isUsed = new HashMap<>();
    for (String itemId : targetCourse.get().getCourseItemIds()) {
      isUsed.put(itemId, true);
    }

    for (String itemId : orderOfItemIds) {
      if (!isUsed.containsKey(itemId)) {
        return res
            .errorMessage("The item ID does not exist in the provided course's item list.")
            .errorType(BackendOperationErrors.ItemNotFound)
            .build();
      }
    }

    try {
      targetCourse.get().setCourseItemIds(orderOfItemIds);
      courseRepo.save(targetCourse.get());
    } catch (Exception e) {
      log.error("Failed to update the order of the course item IDs. Reason: {}", e.toString());

      return res
          .errorMessage("Failed to update the order of course items.")
          .errorType(BackendOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemId itemId = new ItemId(targetCourse.get().getId().toString());

    Optional<String> respJson = jsonSerializer.serialize(itemId);
    if (respJson.isEmpty()) {
      return res
          .errorMessage("Failed to serialize response JSON.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return res
        .result(respJson.get())
        .build();
  }

  public Optional<Course> getCourse(UUID courseId) {
    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      log.info("No course exists with ID: {}", courseId);
    }

    return targetCourse;
  }

  // Instructors can update everything except the following:
  // totalStars, usersWhoRated, createdAt, lastModifiedAt
  // Also, they can only publish a course. Once published, they can't undo it.
  public Optional<BackendOperationErrors> updateCourse(CourseModificationRequest courseModificationRequest, UUID currentUserId) {
    Optional<String> invalidReason = this.basicCourseModificationRequestValidation(courseModificationRequest);
    if (invalidReason.isPresent()) {
      return Optional.of(BackendOperationErrors.InvalidRequest);
    }

    Optional<Course> courseToBeModified = courseRepo.findCourseById(courseModificationRequest.getCourseId());
    if (courseToBeModified.isEmpty()) {
      return Optional.of(BackendOperationErrors.CourseNotFound);
    }
    if (!courseToBeModified.get().getInstructorId().equals(currentUserId)) {
      log.info("Course Instructor: {}\nCurrent User: {}", courseToBeModified.get().getInstructorId(), currentUserId);
      return Optional.of(BackendOperationErrors.AttemptingToModifyOthersItem);
    }

    if (courseModificationRequest.getTitle() != null) {
      courseToBeModified.get().setTitle(courseModificationRequest.getTitle().trim());
    }
    if (courseModificationRequest.getDescription() != null) {
      courseToBeModified.get().setDescription(courseModificationRequest.getDescription().trim());
    }
    if (courseModificationRequest.getPrice() != null) {
      courseToBeModified.get().setPrice(courseModificationRequest.getPrice());
    }
    courseToBeModified.get().setLastModifiedAt(LocalDateTime.now());

    courseRepo.save(courseToBeModified.get());
    return Optional.empty();
  }

  public ResultOrError<Boolean, BackendOperationErrors> updateCourseThumbnail(
      UUID courseId,
      UUID userId,
      String newThumbnailFilename
  ) {
    var res = ResultOrError.<Boolean, BackendOperationErrors>builder();

    Optional<Course> courseToBeModified = courseRepo.findCourseById(courseId);
    if (courseToBeModified.isEmpty()) {
      return res
          .result(false)
          .errorMessage("Course not found.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }
    if (!courseToBeModified.get().getInstructorId().equals(userId)) {
      return res
          .result(false)
          .errorMessage("You can't change the thumbnail of the course because you're not the author of this course.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    courseToBeModified.get().setThumbnailImageName(newThumbnailFilename);
    courseToBeModified.get().setLastModifiedAt(LocalDateTime.now());
    meiliService.updateCourseInDocument(courseToBeModified.get());
    courseRepo.save(courseToBeModified.get());

    return res
        .result(true)
        .build();
  }

  public ResultOrError<String, BackendOperationErrors> deleteCourse(UUID courseToDelete, UUID currentUserId) {
    var resp = ResultOrError.<String, BackendOperationErrors>builder();

    Optional<Course> targetCourse = courseRepo.findCourseById(courseToDelete);
    if (targetCourse.isEmpty()) {
      return resp
          .errorMessage("Course with provided ID not found.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return resp
          .errorMessage("Attempting to delete the course that does not belong to the user.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    if (targetCourse.get().getIsPublished()) {
      return resp
          .errorMessage("Can't remove an already published course.")
          .errorType(BackendOperationErrors.InvalidRequest)
          .build();
    }

    courseRepo.deleteById(courseToDelete);

    ItemId deletedCourseId = new ItemId(targetCourse.get().getId().toString());
    Optional<String> respJson = jsonSerializer.serialize(deletedCourseId);
    if (respJson.isEmpty()) {
      return resp
          .errorMessage("Failed to serialize response JSON.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return resp
        .result(respJson.get())
        .build();
  }

  public Optional<String> publishCourse(UUID courseId, UUID currentUserId) {
    if (courseId == null) {
      return Optional.of("Course id must be provided to publish a course");
    }
    if (currentUserId == null) {
      return Optional.of("Current user id must be provided to publish a course");
    }

    Optional<Course> courseToPublish = courseRepo.findCourseById(courseId);
    if (courseToPublish.isEmpty()) {
      return Optional.of("Course by that id does not exist");
    }
    if (courseToPublish.get().getIsPublished()) {
      // The course is already published. Nothing left to do.
      return Optional.empty();
    }
    if (!currentUserId.equals(courseToPublish.get().getInstructorId())) {
      return Optional.of("Current user isn't the author of the course they are trying to publish");
    }

    courseToPublish.get().setIsPublished(true);
    courseToPublish.get().setLastModifiedAt(LocalDateTime.now());
    courseRepo.save(courseToPublish.get());

    Optional<Users> instructor = userService.findUserById(courseToPublish.get().getInstructorId());
    if (instructor.isEmpty()) {
      return Optional.of("Failed to retrieve instructor account info.");
    }

    meiliService.addCourseToIndex(courseToPublish.get(), instructor.get().getUsername());

    return Optional.empty();
  }

  public Optional<String> addRating(UUID courseId, int starRating) {
    Optional<String> invalidReason = Optional.empty();

    if (courseId == null) {
      invalidReason = Optional.of("Course id must be provided to add a starRating");
    }
    if (starRating < 0 || starRating > 5) {
      invalidReason = Optional.of("Rating must be between 0 and 5");
    }

    Optional<Course> courseToBeReviewed = courseRepo.findCourseById(courseId);
    if (courseToBeReviewed.isEmpty()) {
      invalidReason = Optional.of("Course by that id does not exist");
    }

    if (invalidReason.isPresent()) {
      return invalidReason;
    }

    courseToBeReviewed.get().setTotalStars(courseToBeReviewed.get().getTotalStars() + starRating);
    courseToBeReviewed.get().setUsersWhoRatedCount(courseToBeReviewed.get().getUsersWhoRatedCount() + 1);
    courseToBeReviewed.get().setLastModifiedAt(LocalDateTime.now());
    courseRepo.save(courseToBeReviewed.get());

    return invalidReason;
  }

  public PaginatedCourses<CleanedCourse> getAllCoursesPaginated(int N, int pageNumber, Sort.Direction sorting) {
    var paginatedCourses = PaginatedCourses.<CleanedCourse>builder();

    if (N < 1) {
      paginatedCourses
          .retrievedCourseCount(0)
          .retrievedCourses(null)
          .pageNumber(pageNumber)
          .pageSize(N)
          .totalPaginatedPages(0);

      return paginatedCourses.build();
    }

    Page<Course> page = courseRepo.findAll(
        PageRequest.of(pageNumber, N, Sort.by(sorting, "createdAt"))
    );

    List<CleanedCourse> content = new ArrayList<>();
    page.getContent().forEach(course -> {
      content.add(new CleanedCourse(course));
    });

    paginatedCourses
        .retrievedCourseCount(content.size())
        .retrievedCourses(content)
        .pageNumber(pageNumber)
        .pageSize(N)
        .totalPaginatedPages(page.getTotalPages());

    return paginatedCourses.build();
  }

  public PaginatedCourses<CleanedCoursesForInstructors> getInstructorCoursesPaginated(String instructorId, int pageSize, int pageNumber, Sort.Direction sortingDirection) {
    var paginatedCourses = PaginatedCourses.<CleanedCoursesForInstructors>builder();
    if (pageSize < 1) {
      paginatedCourses
          .retrievedCourseCount(0)
          .retrievedCourses(null)
          .pageNumber(pageNumber)
          .pageSize(pageSize)
          .totalPaginatedPages(0);

      return paginatedCourses.build();
    }

    Page<Course> page = courseRepo.findAllByInstructorId(
        UUID.fromString(instructorId),
        PageRequest.of(pageNumber, pageSize, Sort.by(sortingDirection, "createdAt"))
    );

    List<CleanedCoursesForInstructors> content = new ArrayList<>();
    page.getContent().forEach(course -> {
      content.add(new CleanedCoursesForInstructors(course));
    });

    paginatedCourses
        .retrievedCourseCount(content.size())
        .retrievedCourses(content)
        .pageNumber(pageNumber)
        .pageSize(pageSize)
        .totalPaginatedPages(page.getTotalPages());

    return paginatedCourses.build();
  }

  public PaginatedCourses<CleanedCourse> getLearnerCoursesPaginated(String userId, int pageSize, int pageNumber, Sort.Direction sortingDirection) {
    var paginatedCourses = PaginatedCourses.<CleanedCourse>builder();

    if (pageSize < 1) {
      paginatedCourses
          .retrievedCourseCount(0)
          .retrievedCourses(null)
          .pageNumber(pageNumber)
          .pageSize(pageSize)
          .totalPaginatedPages(0);

      return paginatedCourses.build();
    }

    Page<Purchase> userPurchases = purchaseRepo.findPurchaseByBuyerId(
        UUID.fromString(userId),
        PageRequest.of(pageNumber, pageSize, Sort.by(sortingDirection, "purchaseTimestamp"))
    );
    if (userPurchases.isEmpty()) {
      paginatedCourses
          .retrievedCourseCount(0)
          .retrievedCourses(null)
          .pageNumber(pageNumber)
          .pageSize(pageSize)
          .totalPaginatedPages(0);

      return paginatedCourses.build();
    }

    ArrayList<CleanedCourse> purchasedCourses = new ArrayList<>();
    userPurchases.forEach(purchase -> {
      Optional<Course> targetCourse = courseRepo.findCourseById(purchase.getCourseId());
      targetCourse.ifPresent((course) -> {
        purchasedCourses.add(new CleanedCourse(course));
      });
    });

    paginatedCourses
        .retrievedCourseCount(purchasedCourses.size())
        .retrievedCourses(purchasedCourses)
        .pageNumber(pageNumber)
        .pageSize(pageSize)
        .totalPaginatedPages(userPurchases.getTotalPages());

    return paginatedCourses.build();
  }

  public ResultOrError<String, BackendOperationErrors> addTags(UUID userId, UUID courseId, List<String> tagsToAdd) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();
    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return res
          .errorMessage("Course not found.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    Optional<Users> targetUser = userService.findUserById(userId);
    if (targetUser.isEmpty()) {
      return res
          .errorMessage("User not found.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(userId)) {
      return res
          .errorMessage("Failed to add tag(s). Can't add tag(s) to a course that isn't yours.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    targetCourse.get().getTags().addAll(tagsToAdd);
    courseRepo.save(targetCourse.get());
    meiliService.updateCourseInDocument(targetCourse.get());

    ItemId resp = new ItemId(courseId.toString());
    Optional<String> respJson = jsonSerializer.serialize(resp);
    if (respJson.isEmpty()) {
      return res
          .errorMessage("Failed to serialize JSON response.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return res
        .result(respJson.get())
        .build();
  }

  public ResultOrError<String, BackendOperationErrors> modifyTags(UUID userId, UUID courseId, List<String> modifiedTagList) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();
    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return res
          .errorMessage("Course not found.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    Optional<Users> targetUser = userService.findUserById(userId);
    if (targetUser.isEmpty()) {
      return res
          .errorMessage("User not found.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(userId)) {
      return res
          .errorMessage("Failed to modify the tag(s). Can't modify the tag(s) of a course that isn't yours.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    targetCourse.get().setTags(modifiedTagList);
    courseRepo.save(targetCourse.get());
    meiliService.updateCourseInDocument(targetCourse.get());

    ItemId resp = new ItemId(courseId.toString());
    Optional<String> respJson = jsonSerializer.serialize(resp);
    if (respJson.isEmpty()) {
      return res
          .errorMessage("Failed to serialize JSON response.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return res
        .result(respJson.get())
        .build();
  }

  public ResultOrError<String, BackendOperationErrors> deleteTags(UUID userId, UUID courseId, List<String> tagsToDelete) {
    var res = ResultOrError.<String, BackendOperationErrors>builder();
    Optional<Course> targetCourse = courseRepo.findCourseById(courseId);
    if (targetCourse.isEmpty()) {
      return res
          .errorMessage("Course not found.")
          .errorType(BackendOperationErrors.CourseNotFound)
          .build();
    }

    Optional<Users> targetUser = userService.findUserById(userId);
    if (targetUser.isEmpty()) {
      return res
          .errorMessage("User not found.")
          .errorType(BackendOperationErrors.ItemNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(userId)) {
      return res
          .errorMessage("Failed to delete the tag(s). Can't delete the tag(s) of a course that isn't yours.")
          .errorType(BackendOperationErrors.AttemptingToModifyOthersItem)
          .build();
    }

    targetCourse.get().getTags().removeAll(tagsToDelete);
    courseRepo.save(targetCourse.get());
    meiliService.updateCourseInDocument(targetCourse.get());

    ItemId resp = new ItemId(courseId.toString());
    Optional<String> respJson = jsonSerializer.serialize(resp);
    if (respJson.isEmpty()) {
      return res
          .errorMessage("Failed to serialize JSON response.")
          .errorType(BackendOperationErrors.FailedToSerializeJson)
          .build();
    }

    return res
        .result(respJson.get())
        .build();
  }
}
