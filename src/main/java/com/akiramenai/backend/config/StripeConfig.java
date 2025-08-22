package com.akiramenai.backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class StripeConfig {
  @Value("${application.stripe.secret-key}")
  private String secretKey;

  @PostConstruct
  public void init() {
    Stripe.apiKey = this.secretKey;
  }
}
