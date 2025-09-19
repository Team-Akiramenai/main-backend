package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CompletedCourseItemsRepo;
import com.akiramenai.backend.repo.InstructorInfosRepo;
import com.akiramenai.backend.repo.LearnerInfosRepo;
import com.akiramenai.backend.repo.PurchaseRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.LoginActivityService;
import com.akiramenai.backend.service.UserService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.IdParser;
import com.akiramenai.backend.utility.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
public class AnalyticsController {
  private final LoginActivityService loginActivityService;
  private final CompletedCourseItemsRepo completedCourseItemsRepo;
  HttpResponseWriter responseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final UserService userService;
  private final InstructorInfosRepo instructorInfosRepo;
  private final PurchaseRepo purchaseRepo;

  public AnalyticsController(
      UserService userService,
      InstructorInfosRepo instructorInfosRepo,
      PurchaseRepo purchaseRepo,
      LoginActivityService loginActivityService, CompletedCourseItemsRepo completedCourseItemsRepo) {
    this.userService = userService;
    this.instructorInfosRepo = instructorInfosRepo;
    this.purchaseRepo = purchaseRepo;
    this.loginActivityService = loginActivityService;
    this.completedCourseItemsRepo = completedCourseItemsRepo;
  }

  private record DateRange(LocalDateTime start, LocalDateTime end) {
  }

  private DateRange getDateRange(Optional<Integer> year, Optional<Integer> month) {
    LocalDateTime ldtNow = LocalDateTime.now();
    LocalDateTime ldtStart = null;
    LocalDateTime ldtEnd = null;
    if (year.isEmpty() && month.isEmpty()) {
      // data for this month
      ldtStart = ldtNow.withDayOfMonth(1);
      ldtEnd = ldtStart.plusMonths(1).minusDays(1);
    } else if (month.isEmpty()) {
      // data for provided year
      ldtStart = LocalDateTime.of(year.get(), 1, 1, 0, 0, 0);
      ldtEnd = ldtStart.plusYears(1).minusDays(1);
    } else if (year.isEmpty()) {
      // data for provided month of this year
      ldtStart = LocalDateTime.of(ldtNow.getYear(), month.get(), 1, 0, 0, 0);
      ldtEnd = ldtStart.plusMonths(1).minusDays(1);
    } else {
      // data of provided month of the provided year
      ldtStart = LocalDateTime.of(year.get(), month.get(), 1, 0, 0, 0);
      ldtEnd = ldtStart.plusMonths(1).minusDays(1);
    }

    return new DateRange(ldtStart, ldtEnd);
  }

  @GetMapping("api/protected/get/instructor-analytics")
  public void getInstructorAnalytics(
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    UUID userId = UUID.fromString(request.getAttribute("userId").toString());
    String accountType = request.getAttribute("accountType").toString();

    if (!accountType.equals("Instructor")) {
      responseWriter.writeFailedResponse(response, "Only instructors can access this endpoint.", HttpStatus.FORBIDDEN);
      return;
    }

    Optional<Users> instructorUser = userService.findUserById(userId);
    if (instructorUser.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to get instructor account details.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    Optional<InstructorInfos> currentInstructor = instructorInfosRepo.getInstructorInfosById(instructorUser.get().getInstructorForeignKey());
    if (currentInstructor.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to retrieve instructor's account information.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    Optional<List<Integer>> activitiesThisMonth = loginActivityService.getLoginActivityForMonth(userId, LocalDate.now());
    if (activitiesThisMonth.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Login activity for this month not found.", HttpStatus.NOT_FOUND);
      return;
    }

    InstructorAnalyticsResponse responseObj =
        InstructorAnalyticsResponse
            .builder()
            .loginStreak(instructorUser.get().getLoginStreak())
            .loginActivity(activitiesThisMonth.get())
            .totalCoursesSold(currentInstructor.get().getTotalCoursesSold())
            .accountBalance(currentInstructor.get().getAccountBalance())
            .totalAvailableStorage(instructorUser.get().getTotalStorageInBytes())
            .usedStorage(instructorUser.get().getUsedStorageInBytes())
            .build();

    Optional<String> responseJson = jsonSerializer.serialize(responseObj);
    if (responseJson.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    responseWriter.writeOkResponse(response, responseJson.get(), HttpStatus.OK);
  }

  @GetMapping("api/protected/get/courses-sold/in")
  public void getCoursesSold(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam(name = "year", required = false) Optional<Integer> year,
      @RequestParam(name = "month", required = false) Optional<Integer> month
  ) {

    DateRange dateRange = getDateRange(year, month);

    String userId = request.getAttribute("userId").toString();
    String accountType = request.getAttribute("accountType").toString();

    if (!accountType.equals("Instructor")) {
      responseWriter.writeFailedResponse(response, "Only instructors can access this endpoint.", HttpStatus.FORBIDDEN);
      return;
    }

    List<Purchase> purchasesMade = purchaseRepo.findAllByAuthorIdAndPurchaseTimestampBetween(
        UUID.fromString(userId),
        dateRange.start,
        dateRange.end
    );

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    List<CourseSellDatapoint> generatedDatapoints = new ArrayList<>();
    Map<String, double[]> count = new HashMap<>();
    purchasesMade.forEach(p -> {
      if (!count.containsKey(p.getPurchaseDate().format(formatter))) {
        count.put(p.getPurchaseDate().format(formatter), new double[]{0.0, 0.0});
      }

      count.get(p.getPurchaseDate().format(formatter))[0]++;
      count.get(p.getPurchaseDate().format(formatter))[1] += p.getPrice();
    });

    count.forEach((s, d) -> {
      CourseSellDatapoint dp =
          CourseSellDatapoint
              .builder()
              .date(s)
              .coursesSold((long) d[0])
              .revenueGenerated(d[1])
              .build();

      generatedDatapoints.add(dp);
    });

    CourseAnalyticsResponse res =
        CourseAnalyticsResponse
            .builder()
            .daysCovered(Duration.between(dateRange.start, dateRange.end).toDays() + 1)
            .startDate(dateRange.start.format(formatter))
            .endDate(dateRange.end.format(formatter))
            .datapoints(generatedDatapoints)
            .build();

    CoursesSoldResponse responseObj =
        CoursesSoldResponse
            .builder()
            .coursesSold(purchasesMade.size())
            .build();

    Optional<String> responseJson = jsonSerializer.serialize(res);
    if (responseJson.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    responseWriter.writeOkResponse(response, responseJson.get(), HttpStatus.OK);
  }

  @GetMapping("api/protected/get/learner-analytics")
  public void getLearnerAnalytics(
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    UUID userId = UUID.fromString(request.getAttribute("userId").toString());
    String accountType = request.getAttribute("accountType").toString();

    log.info("Account type is: " + accountType);

    if (!accountType.equals("Learner")) {
      responseWriter.writeFailedResponse(response, "Only learners can access this API endpoint.", HttpStatus.NOT_FOUND);
      return;
    }

    Optional<Users> targetUser = userService.findUserById(userId);
    if (targetUser.isEmpty()) {
      responseWriter.writeFailedResponse(response, "User not found.", HttpStatus.NOT_FOUND);
      return;
    }

    Optional<List<Integer>> activitiesThisMonth = loginActivityService.getLoginActivityForMonth(userId, LocalDate.now());
    if (activitiesThisMonth.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Login activity for this month not found.", HttpStatus.NOT_FOUND);
      return;
    }

    LearnerAnalyticsResponse resp = LearnerAnalyticsResponse
        .builder()
        .loginStreak(targetUser.get().getLoginStreak())
        .activityThisMonth(activitiesThisMonth.get())
        .build();

    Optional<String> responseJson = jsonSerializer.serialize(resp);
    if (responseJson.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    responseWriter.writeOkResponse(response, responseJson.get(), HttpStatus.OK);
  }


  @GetMapping("api/protected/get/user/login-activity")
  public void getLoginActivityMonth(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam(name = "year", required = true) int year,
      @RequestParam(name = "month", required = false) Integer monthNumber
  ) {
    UUID userId = UUID.fromString(request.getAttribute("userId").toString());

    Optional<List<Integer>> activitiesToReturn;
    if (monthNumber == null) {
      activitiesToReturn = loginActivityService.getLoginActivityForYear(
          userId,
          LocalDate.of(year, 1, 1)
      );
    } else {
      activitiesToReturn = loginActivityService.getLoginActivityForMonth(
          userId,
          LocalDate.of(year, monthNumber, 1)
      );
    }

    if (activitiesToReturn.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Login activity for this month not found.", HttpStatus.NOT_FOUND);
      return;
    }

    MonthActivityResponse resp = new MonthActivityResponse(activitiesToReturn.get());
    Optional<String> responseJson = jsonSerializer.serialize(resp);
    if (responseJson.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    responseWriter.writeOkResponse(response, responseJson.get(), HttpStatus.OK);
  }

  @GetMapping("api/protected/get/completed-items/analytics")
  public void completedItemAnalytics(
      HttpServletRequest request,
      HttpServletResponse response,

      @RequestParam(name = "item-type", required = true) String itemType,
      @RequestParam(name = "year", required = false) Optional<Integer> year,
      @RequestParam(name = "month", required = false) Optional<Integer> month
  ) {
    UUID userId = UUID.fromString(request.getAttribute("userId").toString());
    String accountType = request.getAttribute("accountType").toString();
    if (!accountType.equals("Learner")) {
      responseWriter.writeFailedResponse(response, "Only learners can access this API endpoint.", HttpStatus.NOT_FOUND);
      return;
    }

    DateRange dateRange = getDateRange(year, month);

    CourseItems requestCourseItem;
    try {
      requestCourseItem = CourseItems.valueOf(itemType);
    } catch (Exception e) {
      responseWriter.writeFailedResponse(
          response,
          "Invalid item type. Accepted ItemType: Video, Quiz, CodingTest, TerminalTest.", HttpStatus.NOT_FOUND
      );
      return;
    }

    Optional<List<CompletedCourseItems>> completedItems =
        completedCourseItemsRepo.findCompletedCourseItemsByLearnerIdAndItemTypeAndCompletedAtBetween(
            userId,
            requestCourseItem,
            dateRange.start.toLocalDate(),
            dateRange.end.toLocalDate()
        );
    if (completedItems.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to retrieve completed items.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    ArrayList<CleanedCompletedItem> cleanedCompletedItems = new ArrayList<>();
    for (CompletedCourseItems cci : completedItems.get()) {
      cleanedCompletedItems.add(new CleanedCompletedItem(cci));
    }

    Optional<String> responseJson = jsonSerializer.serialize(cleanedCompletedItems);
    if (responseJson.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    responseWriter.writeOkResponse(response, responseJson.get(), HttpStatus.OK);
  }
}
