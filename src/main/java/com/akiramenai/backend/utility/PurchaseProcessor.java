package com.akiramenai.backend.utility;

import com.akiramenai.backend.model.PurchaseInfo;
import com.akiramenai.backend.model.Users;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

@Slf4j
public class PurchaseProcessor {
  private final BlockingQueue<PurchaseInfo> purchaseInfoBlockingQueue;
  private final UserRepo userRepo;
  private final CourseService courseService;

  public PurchaseProcessor(
      BlockingQueue<PurchaseInfo> purchaseInfoBlockingQueue,
      UserRepo userRepo,
      CourseService courseService
  ) {
    this.purchaseInfoBlockingQueue = purchaseInfoBlockingQueue;
    this.userRepo = userRepo;
    this.courseService = courseService;
  }

  public void start() {
    new Thread(() -> {
      log.info("Purchase processing thread has started.");

      while (!Thread.currentThread().isInterrupted()) {
        try {
          PurchaseInfo purchaseInfo = purchaseInfoBlockingQueue.take();

          handlePurchase(purchaseInfo);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }).start();
  }

  private void handlePurchase(PurchaseInfo purchaseInfo) {
    switch (purchaseInfo.purchaseTypes()) {
      case Course: {
        addPurchasedCourse(
            UUID.fromString(purchaseInfo.userId()),
            UUID.fromString(purchaseInfo.courseId())
        );
        log.info("Adding course {} to user {}.", purchaseInfo.courseId(), purchaseInfo.userId());
        break;
      }

      case Storage: {
        addPurchasedStorage(UUID.fromString(purchaseInfo.userId()), purchaseInfo.storageToAddInGBs());
        log.info("Added {}GB of storage for user `{}`.", purchaseInfo.storageToAddInGBs(), purchaseInfo.userId());
        break;
      }

      default: {
        log.info("Unknown purchase type `{}`.", purchaseInfo.purchaseTypes());
        break;
      }
    }

  }

  private void addPurchasedStorage(UUID userId, long storageAmountInGBs) {
    Optional<Users> targetUser = userRepo.findUsersById(userId);
    if (targetUser.isEmpty()) {
      log.error("User `{}` not found.", userId);
      return;
    }

    // 1GB = 1073741824 bytes
    long storageBoughtInBytes = storageAmountInGBs * 1073741824L;

    long totalStorageBytes = targetUser.get().getTotalStorageInBytes();
    targetUser.get().setTotalStorageInBytes(totalStorageBytes + storageBoughtInBytes);
    try {
      userRepo.save(targetUser.get());
    } catch (Exception e) {
      log.error("Failed to increase user's storage. Reason: {}", e.getMessage());
      return;
    }
  }

  private void addPurchasedCourse(UUID userId, UUID courseId) {
    Optional<String> resp = courseService.purchaseCourse(
        courseId.toString(),
        userId.toString()
    );
    if (resp.isEmpty()) {
      log.error("Failed to purchase course `{}` for user `{}`.", courseId, userId);
      return;
    }
  }
}
