package com.akiramenai.backend.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class JsonSerializer {
  private final ObjectMapper mapper;

  public JsonSerializer() {
    mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
  }

  public Optional<String> serialize(Object course) {
    try {
      return Optional.of(mapper.writeValueAsString(course));
    } catch (Exception e) {
      log.error("Failed to serialize JSON. Reason: ", e);

      return Optional.empty();
    }
  }
}
