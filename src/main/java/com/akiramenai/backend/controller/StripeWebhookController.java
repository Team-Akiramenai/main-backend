package com.akiramenai.backend.controller;

import com.akiramenai.backend.model.PurchaseTypes;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.service.CourseService;
import com.akiramenai.backend.service.StripeService;
import com.stripe.model.Event;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import com.akiramenai.backend.model.PurchaseInfo;
import com.akiramenai.backend.utility.PurchaseProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
@RestController
public class StripeWebhookController {
  @Value("${application.stripe.webhook-secret}")
  private String webhookSecret;

  private final PurchaseProcessor purchaseProcessor;
  private final BlockingQueue<PurchaseInfo> purchaseInfoBlockingQueue;

  public StripeWebhookController(UserRepo userRepo, CourseService courseService) {
    this.purchaseInfoBlockingQueue = new ArrayBlockingQueue<>(1024);
    this.purchaseProcessor = new PurchaseProcessor(
        this.purchaseInfoBlockingQueue,
        userRepo,
        courseService
    );

    this.purchaseProcessor.start();
  }

  @PostMapping("/webhook")
  public ResponseEntity<String> handleStripeWebhook(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String sigHeader
  ) {
    Event event;
    try {
      event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
    } catch (SignatureVerificationException e) {
      log.error("Signature verification failed. Reason: ", e);
      // Invalid signature
      return new ResponseEntity<>("Invalid webhook signature", HttpStatus.BAD_REQUEST);
    }

    // Handle the event based on its type
    switch (event.getType()) {
      case "checkout.session.completed": {

        // queue a task to a different thread
        Optional<StripeObject> wrappedSession = event.getDataObjectDeserializer().getObject();
        if (wrappedSession.isEmpty()) {
          log.error("Something went wrong while deserializing the event to get the metadata.");
          break;
        }
        Session session = (Session) wrappedSession.get();

        String userId = session.getMetadata().get("userId");
        String itemType = session.getMetadata().get("purchaseTypes");
        String courseId = session.getMetadata().get("courseId");
        String storageToAddInGBs = session.getMetadata().get("storageBoughtInGBs");

        if (itemType.equals("Course")) {
          this.purchaseInfoBlockingQueue.add(
              new PurchaseInfo(userId, PurchaseTypes.Course, courseId, null)
          );
        } else {
          this.purchaseInfoBlockingQueue.add(
              new PurchaseInfo(
                  userId, PurchaseTypes.Storage, null, Integer.parseInt(storageToAddInGBs)
              )
          );
        }


        break;
      }
      default:
        log.info("Unhandled event type: {}", event.getType());
    }

    return new ResponseEntity<>("Webhook received and processed", HttpStatus.OK);
  }
}
