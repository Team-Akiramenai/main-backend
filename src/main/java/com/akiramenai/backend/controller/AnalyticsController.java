package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.InstructorInfosRepo;
import com.akiramenai.backend.repo.LearnerInfosRepo;
import com.akiramenai.backend.repo.PurchaseRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.UserService;
import com.akiramenai.backend.utility.HttpResponseWriter;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
public class AnalyticsController {
  HttpResponseWriter responseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final UserService userService;
  private final InstructorInfosRepo instructorInfosRepo;
  private final PurchaseRepo purchaseRepo;

  public AnalyticsController(
      UserService userService,
      InstructorInfosRepo instructorInfosRepo,
      PurchaseRepo purchaseRepo
  ) {
    this.userService = userService;
    this.instructorInfosRepo = instructorInfosRepo;
    this.purchaseRepo = purchaseRepo;
  }

  @GetMapping("api/protected/get/instructor-analytics")
  public void getInstructorAnalytics(
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    String userId = request.getAttribute("userId").toString();
    String accountType = request.getAttribute("accountType").toString();

    if (!accountType.equals("Instructor")) {
      responseWriter.writeFailedResponse(response, "Only instructors can access this endpoint.", HttpStatus.FORBIDDEN);
      return;
    }

    Optional<Users> instructorUser = userService.findUserById(UUID.fromString(userId));
    if (instructorUser.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to get instructor account details.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    Optional<InstructorInfos> currentInstructor = instructorInfosRepo.getInstructorInfosById(instructorUser.get().getInstructorForeignKey());
    if (currentInstructor.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to retrieve instructor's account information.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    InstructorAnalyticsResponse responseObj =
        InstructorAnalyticsResponse
            .builder()
            .totalCoursesSold(currentInstructor.get().getTotalCoursesSold())
            .accountBalance(currentInstructor.get().getAccountBalance())
            .build();

    Optional<String> responseJson = jsonSerializer.serialize(responseObj);
    if (responseJson.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    responseWriter.writeOkResponse(response, responseJson.get(), HttpStatus.OK);
  }


  @GetMapping("api/protected/get/courses-sold")
  public void getCoursesSold(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "last-N-days", required = false, defaultValue = "7") int lastNDays
  ) {
    if (lastNDays <= 0) {
      responseWriter.writeFailedResponse(response, "The number of last days must be greater than zero.", HttpStatus.BAD_REQUEST);
      return;
    }

    String userId = request.getAttribute("userId").toString();
    String accountType = request.getAttribute("accountType").toString();

    if (!accountType.equals("Instructor")) {
      responseWriter.writeFailedResponse(response, "Only instructors can access this endpoint.", HttpStatus.FORBIDDEN);
      return;
    }

    LocalDateTime ldtNow = LocalDateTime.now();
    LocalDateTime ldtBefore = LocalDateTime.now().minusDays(lastNDays + 1);

    long coursesSoldInPeriod = purchaseRepo.countByAuthorIdAndPurchaseTimestampBetween(
        UUID.fromString(userId),
        ldtBefore,
        ldtNow
    );

    List<Purchase> purchasesMade = purchaseRepo.findAllByAuthorIdAndPurchaseTimestampBetween(
        UUID.fromString(userId),
        ldtBefore,
        ldtNow
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
            .daysCovered(Duration.between(ldtBefore, ldtNow).toDays())
            .startDate(ldtBefore.format(formatter))
            .endDate(ldtNow.format(formatter))
            .datapoints(generatedDatapoints)
            .build();

    CoursesSoldResponse responseObj =
        CoursesSoldResponse
            .builder()
            .coursesSold(coursesSoldInPeriod)
            .build();

    Optional<String> responseJson = jsonSerializer.serialize(res);
    if (responseJson.isEmpty()) {
      responseWriter.writeFailedResponse(response, "Failed to serialize JSON response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    responseWriter.writeOkResponse(response, responseJson.get(), HttpStatus.OK);
  }
}
