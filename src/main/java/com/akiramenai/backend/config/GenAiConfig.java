package com.akiramenai.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

@Configuration
public class GenAiConfig {

  @Value("${application.google.api-key}")
  private String googleApiKey;

  @Bean(destroyMethod = "close")
  public Client genAiClient() {
    return Client
        .builder()
        .apiKey(googleApiKey)
        .build();
  }
}
