package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.*;
import com.akiramenai.backend.repo.*;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
public class SeedController {
  UserRepo userRepo;
  InstructorInfosRepo instructorInfosRepo;
  LearnerInfosRepo learnerInfosRepo;

  UserService userService;

  CourseRepo courseRepo;
  CourseService courseService;

  PurchaseRepo purchaseRepo;

  public SeedController(
      UserRepo userRepo,
      InstructorInfosRepo instructorInfosRepo,
      LearnerInfosRepo learnerInfosRepo,
      UserService userService,
      CourseRepo courseRepo,
      CourseService courseService,
      PurchaseRepo purchaseRepo
  ) {
    this.userRepo = userRepo;
    this.instructorInfosRepo = instructorInfosRepo;
    this.learnerInfosRepo = learnerInfosRepo;
    this.userService = userService;
    this.courseRepo = courseRepo;
    this.courseService = courseService;
    this.purchaseRepo = purchaseRepo;
  }

  List<UUID> publishNCourses(int courseCount, UUID instructorId) {
    List<UUID> publishedCourses = new ArrayList<>();

    for (int i = 1; i <= courseCount; i++) {
      AddCourseRequest addCourseRequest = new AddCourseRequest(
          "Course Title #" + i,
          "Course Description #" + i,
          4.20
      );

      ResultOrError<String, BackendOperationErrors> resp = courseService.addCourse(
          addCourseRequest,
          instructorId
      );
      if (resp.errorType() != null) {
        log.error("Failed to add course. Reason: {}", resp.errorMessage());

        System.exit(1);
      }

      ItemId itemId;
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        itemId = objectMapper.readValue(resp.result(), ItemId.class);
      } catch (Exception e) {
        log.error("Failed parse JSON. Reason: {}", String.valueOf(e));

        System.exit(1);
        return null; // without this line the LSP shows warning for impossible condition. The LSP isn't smart enough lol.
      }

      Optional<String> status = courseService.publishCourse(UUID.fromString(itemId.itemId()), instructorId);
      if (status.isPresent()) {
        log.info("Failed to publish course. Reason: {}", status.get());

        System.exit(1);
      }
      publishedCourses.add(UUID.fromString(itemId.itemId()));
    }

    return publishedCourses;
  }

  @GetMapping("/api/public/seed")
  public ResponseEntity<String> seed(
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    // Seed a learner and instructor account
    RegisterRequest registerInstructorRequest = new RegisterRequest();
    registerInstructorRequest.setUsername("Amanda");
    registerInstructorRequest.setPassword("12345678");
    registerInstructorRequest.setEmail("amanda@gmail.com");
    registerInstructorRequest.setAccountType("Instructor");

    Optional<String> resp = userService.register(registerInstructorRequest);
    if (resp.isPresent()) {
      return new ResponseEntity<>(resp.get(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    RegisterRequest registerLearnerRequest = new RegisterRequest();
    registerLearnerRequest.setUsername("johnny");
    registerLearnerRequest.setPassword("12345678");
    registerLearnerRequest.setEmail("johnny@gmail.com");
    registerLearnerRequest.setAccountType("Learner");
    resp = userService.register(registerLearnerRequest);
    if (resp.isPresent()) {
      return new ResponseEntity<>(resp.get(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    UUID instructorId = userRepo.findUsersByEmail(registerInstructorRequest.getEmail()).get().getId();
    UUID learnerId = userRepo.findUsersByEmail(registerLearnerRequest.getEmail()).get().getId();

    List<UUID> publishedCourses = publishNCourses(100, instructorId);

    int min = 5; // Inclusive lower bound
    int max = 90; // Inclusive upper bound
    Random random = new Random();
    int randomNumber = random.nextInt(max - min + 1) + min;

    // we'll buy 5 courses
    for (int i = 1; i <= 5; i++) {
      resp = courseService.purchaseCourse(
          publishedCourses.get(randomNumber + i).toString(),
          learnerId.toString(),
          LocalDateTime.now().minusDays(random.nextInt(random.nextInt(max - min + 1) + min))
      );

      if (resp.isPresent()) {
        return new ResponseEntity<>(resp.get(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    return new ResponseEntity<>("Successfully seeded the DB", HttpStatus.OK);
  }
}
