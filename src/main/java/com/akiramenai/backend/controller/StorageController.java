package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.StorageInfoResponse;
import com.akiramenai.backend.model.Users;
import com.akiramenai.backend.repo.LearnerInfosRepo;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.UserService;
import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
public class StorageController {
  private final UserRepo userRepo;
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  private final UserService userService;
  private final LearnerInfosRepo learnerInfosRepo;
  private final CourseService courseService;

  public StorageController(CourseService courseService, UserService userService, LearnerInfosRepo learnerInfosRepo, UserRepo userRepo) {
    this.courseService = courseService;
    this.userService = userService;
    this.learnerInfosRepo = learnerInfosRepo;
    this.userRepo = userRepo;
  }

  private Sort.Direction getSortDirection(String sorting) {
    Sort.Direction sortDirection = Sort.Direction.ASC;
    if (sorting.equals("DESC")) {
      sortDirection = Sort.Direction.DESC;
    }

    return sortDirection;
  }

  @GetMapping("api/private/get/storage-info")
  public void getStorageInfo(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse
  ) {
    String userId = httpRequest.getAttribute("userId").toString();
    String accountType = httpRequest.getAttribute("accountType").toString();

    if (!accountType.equals("Instructor")) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Only instructors can see their storage usage.", HttpStatus.BAD_REQUEST);
      return;
    }

    Optional<Users> targetUser = this.userService.findUserById(UUID.fromString(userId));
    if (targetUser.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "User not found.", HttpStatus.NOT_FOUND);
      return;
    }

    long totalStorageBytes = targetUser.get().getTotalStorageInBytes();
    long usedStorageBytes = targetUser.get().getUsedStorageInBytes();

    StorageInfoResponse storageInfo = new StorageInfoResponse(
        totalStorageBytes,
        usedStorageBytes,
        ((double) usedStorageBytes / (double) totalStorageBytes) * 100.0
    );

    Optional<String> respJson = jsonSerializer.serialize(storageInfo);
    if (respJson.isEmpty()) {
      httpResponseWriter.writeFailedResponse(httpResponse, "Failed to serialize response.", HttpStatus.INTERNAL_SERVER_ERROR);
      return;
    }

    httpResponseWriter.writeOkResponse(httpResponse, respJson.get(), HttpStatus.OK);
  }

}
