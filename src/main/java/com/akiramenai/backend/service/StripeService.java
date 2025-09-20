package com.akiramenai.backend.service;

import com.akiramenai.backend.model.PurchaseTypes;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData;

import com.akiramenai.backend.model.StripeRequest;
import com.akiramenai.backend.model.StripeResponse;

import java.util.UUID;

@Service
public class StripeService {
  public StripeResponse purchase(StripeRequest stripeRequest) throws StripeException {
    LineItem lineItem = LineItem.builder()
        .setQuantity(1L)
        .setPriceData(
            PriceData.builder()
                .setCurrency("usd")
                .setUnitAmount((long) (stripeRequest.cost() * 100))
                .setProductData(
                    ProductData.builder()
                        .setName(stripeRequest.productName())
                        .setDescription(stripeRequest.productDescription())
                        .addImage("https://images2.imgbox.com/b5/37/5sjRimXe_o.png")
                        .build()
                )
                .build()
        )
        .build();

    String successRedirectUrl = null;
    String cancelRedirectUrl = "http://localhost:3000/purchase/failure";
    switch (stripeRequest.itemType()) {
      case PurchaseTypes.Course -> {
        successRedirectUrl = "http://localhost:3000/course/details/" + stripeRequest.courseId();
        cancelRedirectUrl += "?course-id=" + stripeRequest.courseId();
      }
      case PurchaseTypes.Storage -> {
        successRedirectUrl = "http://localhost:3000/profile";
        cancelRedirectUrl += "?storage-amount=" + stripeRequest.courseId();
      }
    }

    SessionCreateParams params = SessionCreateParams.builder()
        .setSuccessUrl(successRedirectUrl)
        .setCancelUrl(cancelRedirectUrl + "&item-type=" + stripeRequest.itemType())
        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
        .setMode(SessionCreateParams.Mode.PAYMENT)
        .addLineItem(lineItem)

        .putMetadata("userId", stripeRequest.buyerId())
        .putMetadata("purchaseTypes", stripeRequest.itemType().toString())
        .putMetadata("courseId", (stripeRequest.courseId() == null) ? "-1" : stripeRequest.courseId())
        .putMetadata("storageBoughtInGBs", (stripeRequest.storageAmountInGBs() == null) ? "-1" : stripeRequest.storageAmountInGBs().toString())

        .build();

    Session session = Session.create(params);

    return StripeResponse.builder()
        .status("OK")
        .msg("Payment created.")
        .sessionId(session.getId())
        .sessionUrl(session.getUrl())
        .build();
  }
}
