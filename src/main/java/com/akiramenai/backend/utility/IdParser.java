package com.akiramenai.backend.utility;

import java.util.Optional;
import java.util.UUID;

import com.akiramenai.backend.model.CourseItems;
import com.akiramenai.backend.model.ParsedItemInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IdParser {
  public static Optional<ParsedItemInfo> parseItemId(String modifiedItemId) {
    String itemTypeString = modifiedItemId.substring(0, 2);

    UUID uuidString;
    try {
      uuidString = UUID.fromString(modifiedItemId.substring(3));
    } catch (Exception e) {
      log.error("Failed to parse UUID from the provided String.");

      return Optional.empty();
    }

    switch (itemTypeString) {
      case "VM":
        return Optional.of(new ParsedItemInfo(CourseItems.Video, uuidString));
      case "QZ":
        return Optional.of(new ParsedItemInfo(CourseItems.Quiz, uuidString));
      case "CT":
        return Optional.of(new ParsedItemInfo(CourseItems.CodingTest, uuidString));
      case "TT":
        return Optional.of(new ParsedItemInfo(CourseItems.TerminalTest, uuidString));

      default:
        return Optional.empty();
    }
  }

  public static Optional<UUID> parseId(String stringId) {
    try {
      return Optional.of(UUID.fromString(stringId));
    } catch (Exception e) {
      log.error("Failed to parse UUID from the provided String. Reason: {}", e.getMessage());

      return Optional.empty();
    }
  }
}
