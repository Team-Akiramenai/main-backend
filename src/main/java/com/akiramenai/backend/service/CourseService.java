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
  private final LearnerInfosRepo learnerInfosRepo;
  private final UserService userService;
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final CourseRepo courseRepo;
  private final PurchaseRepo purchaseRepo;
  private final InstructorInfosService instructorInfosService;

  public CourseService(CourseRepo courseRepo, PurchaseRepo purchaseRepo, InstructorInfosService instructorInfosService, LearnerInfosRepo learnerInfosRepo, UserService userService) {
    this.courseRepo = courseRepo;
    this.purchaseRepo = purchaseRepo;
    this.instructorInfosService = instructorInfosService;
    this.learnerInfosRepo = learnerInfosRepo;
    this.userService = userService;
  }

  private Optional<String> basicCourseValidation(Course course) {
    course.setTitle(course.getTitle().trim());
    course.setDescription(course.getDescription().trim());

    if (
        course.getTitle() == null || course.getDescription() == null
            || course.getTitle().isBlank() || course.getDescription().isBlank()
    ) {
      return Optional.of("Course title and description must be provided to add a course");
    }

    if (course.getTitle().length() > 200) {
      return Optional.of("Course title can't have more than 200 characters");
    }
    if (course.getDescription().length() > 2000) {
      return Optional.of("Course description can't have more than 2000 characters");
    }
    if (course.getPrice() < 1.0) {
      return Optional.of("A course must have a minimum price of 1 dollar (USD)");
    }
    if (course.getCreatedAt() == null || course.getLastModifiedAt() == null) {
      return Optional.of("A course must have a `createdAt` and `lastModified` timestamp");
    }

    return Optional.empty();
  }

  private Optional<String> basicCourseModificationValidation(CourseModifications courseModifications) {
    if (courseModifications.getId() == null) {
      return Optional.of("Course ID must be provided to modify a course");
    }

    if (courseModifications.getTitle() != null) {
      courseModifications.setTitle(courseModifications.getTitle().trim());
      if (courseModifications.getTitle().length() > 200) {
        return Optional.of("Course title can't have more than 200 characters");
      }
    }
    if (courseModifications.getDescription() != null) {
      courseModifications.setDescription(courseModifications.getDescription().trim());
      if (courseModifications.getDescription().length() > 2000) {
        return Optional.of("Course description can't have more than 2000 characters");
      }
    }

    if (courseModifications.getPrice() != null) {
      if (courseModifications.getPrice() < 1.0) {
        return Optional.of("A course must have a minimum price of 1 dollar (USD)");
      }
    }

    return Optional.empty();
  }

  public ResultOrError<String, CourseItemOperationErrors> addCourse(AddCourseRequest addCourseRequest, UUID currentUserId) {
    var result = ResultOrError.<String, CourseItemOperationErrors>builder();

    if (addCourseRequest.title() == null || addCourseRequest.title().isBlank()) {
      return result
          .errorMessage("Title can't be blank.")
          .errorType(CourseItemOperationErrors.InvalidRequest)
          .build();
    }
    if (addCourseRequest.description() == null || addCourseRequest.description().isBlank()) {
      return result
          .errorMessage("Description can't be blank.")
          .errorType(CourseItemOperationErrors.InvalidRequest)
          .build();
    }
    if (addCourseRequest.price() <= 1.0) {
      return result
          .errorMessage("Price has to be greater than 1.0 USD.")
          .errorType(CourseItemOperationErrors.InvalidRequest)
          .build();
    }

    LocalDateTime ldtNow = LocalDateTime.now();
    Course courseToAdd = Course
        .builder()
        .instructorId(currentUserId)
        .title(addCourseRequest.title().trim())
        .description(addCourseRequest.description().trim())
        .thumbnailImageName(null)
        .courseItemIds(new ArrayList<>())
        .price(addCourseRequest.price())
        .totalStars(0L)
        .usersWhoRatedCount(0L)
        .createdAt(ldtNow)
        .lastModifiedAt(ldtNow)
        .isPublished(false)
        .build();

    try {
      courseRepo.save(courseToAdd);
    } catch (Exception e) {
      log.error("Error saving course. Reason: ", e);

      return result
          .errorMessage("Failed to save course.")
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemIdResponse itemIdResponse = new ItemIdResponse(courseToAdd.getId().toString());
    Optional<String> respJson = jsonSerializer.serialize(itemIdResponse);
    if (respJson.isEmpty()) {
      return result
          .errorMessage("Failed to serialize response JSON.")
          .errorType(CourseItemOperationErrors.FailedToSerializeJson)
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

  public ResultOrError<String, CourseItemOperationErrors> updateCourseItemOrder(
      String courseId,
      List<String> orderOfItemIds,
      UUID currentUserId
  ) {
    var res = ResultOrError.<String, CourseItemOperationErrors>builder();

    if (courseId == null) {
      return res
          .errorMessage("No course ID provided.")
          .errorType(CourseItemOperationErrors.InvalidRequest)
          .build();
    }
    if (orderOfItemIds == null) {
      return res
          .errorMessage("Did not provide order of item IDs.")
          .errorType(CourseItemOperationErrors.InvalidRequest)
          .build();
    }

    UUID courseUUID;
    try {
      courseUUID = UUID.fromString(courseId);
    } catch (Exception e) {
      log.error("Invalid UUID provided for course ID. Reason: {}", e.toString());

      return res
          .errorMessage("Provided course ID is an invalid UUID.")
          .errorType(CourseItemOperationErrors.InvalidRequest)
          .build();
    }

    Optional<Course> targetCourse = courseRepo.findById(courseUUID);
    if (targetCourse.isEmpty()) {
      return res
          .errorMessage("No course with that ID exists.")
          .errorType(CourseItemOperationErrors.CourseNotFound)
          .build();
    }

    if (!targetCourse.get().getInstructorId().equals(currentUserId)) {
      return res
          .errorMessage("Failed to update the ordering of course items. You're not the author of this course.")
          .errorType(CourseItemOperationErrors.AttemptingToModifyOthersCourse)
          .build();
    }

    if (targetCourse.get().getCourseItemIds().size() != orderOfItemIds.size()) {
      return res
          .errorMessage("The number of course item IDs don't match.")
          .errorType(CourseItemOperationErrors.InvalidRequest)
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
            .errorType(CourseItemOperationErrors.ItemNotFound)
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
          .errorType(CourseItemOperationErrors.FailedToSaveToDb)
          .build();
    }

    ItemIdResponse itemIdResponse = new ItemIdResponse(targetCourse.get().getId().toString());

    Optional<String> respJson = jsonSerializer.serialize(itemIdResponse);
    if (respJson.isEmpty()) {
      return res
          .errorMessage("Failed to serialize response JSON.")
          .errorType(CourseItemOperationErrors.FailedToSerializeJson)
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
  // Also, they can only publish a course, they can't undo it
  public Optional<String> updateCourse(CourseModifications courseModifications, UUID currentUserId) {
    Optional<String> invalidReason = this.basicCourseModificationValidation(courseModifications);
    if (invalidReason.isPresent()) {
      return invalidReason;
    }

    Optional<Course> courseToBeModified = courseRepo.findCourseById(courseModifications.getId());
    if (courseToBeModified.isEmpty()) {
      return Optional.of("Course by that id does not exist");
    }

    if (courseToBeModified.get().getInstructorId() != currentUserId) {
      return Optional.of("Current user isn't the author of the course they are trying to modify");
    }

    if (courseModifications.getTitle() != null) {
      courseToBeModified.get().setTitle(courseModifications.getTitle());
    }
    if (courseModifications.getDescription() != null) {
      courseToBeModified.get().setDescription(courseModifications.getDescription());
    }
    if (courseModifications.getPrice() != null) {
      courseToBeModified.get().setPrice(courseModifications.getPrice());
    }

    courseRepo.save(courseToBeModified.get());
    return Optional.empty();
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

  public Optional<List<CleanedCourse>> getAllCoursesPaginated(int N, int pageNumber, Sort.Direction sorting) {
    if (N < 1) {
      return Optional.empty();
    }

    Page<Course> page = courseRepo.findAll(
        PageRequest.of(pageNumber, N, Sort.by(sorting, "createdAt"))
    );

    List<CleanedCourse> content = new ArrayList<>();

    page.getContent().forEach(course -> {
      content.add(new CleanedCourse(course));
    });

    return Optional.of(content);
  }

  public Optional<List<CleanedCourse>> getInstructorCoursesPaginated(String instructorId, int pageSize, int pageNumber, Sort.Direction sortingDirection) {
    if (pageSize < 1) {
      return Optional.empty();
    }

    Page<Course> page = courseRepo.findAllByInstructorId(
        UUID.fromString(instructorId),
        PageRequest.of(pageNumber, pageSize, Sort.by(sortingDirection, "createdAt"))
    );

    List<CleanedCourse> content = new ArrayList<>();

    page.getContent().forEach(course -> {
      content.add(new CleanedCourse(course));
    });

    return Optional.of(content);
  }

  public Optional<List<CleanedCourse>> getLearnerCoursesPaginated(String userId, int pageSize, int pageNumber, Sort.Direction sortingDirection) {
    if (pageSize < 1) {
      return Optional.empty();
    }

    Page<Purchase> userPurchases = purchaseRepo.findPurchaseByBuyerId(
        UUID.fromString(userId),
        PageRequest.of(pageNumber, pageSize, Sort.by(sortingDirection, "purchaseTimestamp"))
    );
    if (userPurchases.isEmpty()) {
      return Optional.empty();
    }

    ArrayList<CleanedCourse> purchasedCourses = new ArrayList<>();
    userPurchases.forEach(purchase -> {
      Optional<Course> targetCourse = courseRepo.findCourseById(purchase.getCourseId());
      targetCourse.ifPresent((course) -> {
        purchasedCourses.add(new CleanedCourse(course));
      });
    });

    return Optional.of(purchasedCourses);
  }
}
