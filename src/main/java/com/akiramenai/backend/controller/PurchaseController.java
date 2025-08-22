package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.CourseRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.StripeService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class PurchaseController {
  private final HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  private final JsonSerializer jsonSerializer = new JsonSerializer();

  private final StripeService stripeService;
  private final CourseService courseService;

  public PurchaseController(StripeService stripeService, CourseService courseService) {
    this.stripeService = stripeService;
    this.courseService = courseService;
  }

  @PostMapping("/api/protected/purchase/storage")
  public void purchaseStorage(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestBody PurchaseStorageRequest purchaseStorageRequest
  ) {
    String userId = httpRequest.getAttribute("userId").toString();
    String accountType = httpRequest.getAttribute("accountType").toString();

    if (!accountType.equals("Instructor")) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Only instructors can purchase storage.", HttpStatus.BAD_REQUEST);
      return;
    }

    if (purchaseStorageRequest.amountInGBs() < 1) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Purchased storage amount must be at least 1GB.", HttpStatus.BAD_REQUEST);
      return;
    }

    StripeRequest purchaseReq = new StripeRequest(
        String.format("Storage-%dGB", purchaseStorageRequest.amountInGBs()),
        String.format("Extend user storage by %dGBs.", purchaseStorageRequest.amountInGBs()),
        purchaseStorageRequest.amountInGBs() * 1.50, // we charge 1.50 USD for each GB of storage

        userId,
        PurchaseTypes.Storage,
        null,
        purchaseStorageRequest.amountInGBs()
    );
    try {
      StripeResponse resp = this.stripeService.purchase(purchaseReq);
      Optional<String> respJson = jsonSerializer.serialize(resp);
      if (respJson.isEmpty()) {
        httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
        return;
      }

      httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
    } catch (Exception e) {
      log.error("Error purchasing storage. Reason: ", e);

      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to purchase storage.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }
  }


  @PostMapping("/api/protected/purchase/course")
  public void purchaseCourse(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      @RequestBody CoursePurchaseRequest coursePurchaseRequest
  ) {
    String userId = httpRequest.getAttribute("userId").toString();
    String accountType = httpRequest.getAttribute("accountType").toString();

    if (!accountType.equals("Learner")) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Only learners can purchase a course.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<Course> targetCourse = courseService.getCourse(UUID.fromString(coursePurchaseRequest.courseId()));
    if (targetCourse.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to find course by ID.", HttpStatus.NOT_FOUND);
      return;
    }

    StripeRequest purchaseReq = new StripeRequest(
        targetCourse.get().getTitle(),
        targetCourse.get().getDescription(),
        targetCourse.get().getPrice(),

        userId,
        PurchaseTypes.Course,
        coursePurchaseRequest.courseId(),
        null
    );
    try {
      StripeResponse resp = this.stripeService.purchase(purchaseReq);
      Optional<String> respJson = jsonSerializer.serialize(resp);
      if (respJson.isEmpty()) {
        httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize JSON.", HttpStatus.INTERNAL_SERVER_ERROR);
        return;
      }

      httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
    } catch (Exception e) {
      log.error("Error purchasing course. Reason: ", e);

      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to purchase the course.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }
  }

}
