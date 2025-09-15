package com.akiramenai.backend.utility;

import com.akiramenai.backend.model.VttValidationResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VttHelper {

  private static final Pattern LINE_PATTERN =
      Pattern.compile("\\[(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) --> (\\d{2}:\\d{2}:\\d{2}\\.\\d{3})]   (.*)");

  /**
   * Convert timestamp "HH:MM:SS.mmm" into milliseconds.
   */
  private static long parseTimestamp(String ts) {
    String[] parts = ts.split("[:\\.]");
    int hours = Integer.parseInt(parts[0]);
    int minutes = Integer.parseInt(parts[1]);
    int seconds = Integer.parseInt(parts[2]);
    int millis = Integer.parseInt(parts[3]);
    return (hours * 3600L + minutes * 60L + seconds) * 1000L + millis;
  }

  public static VttValidationResult validateVttFile(String filePath) {
    Pattern timestampPattern = Pattern.compile(
        "(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) --> (\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");

    try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
      String line = reader.readLine();
      int lineNumber = 1;

      if (line == null || !line.trim().equals("WEBVTT")) {
        return new VttValidationResult(false,
            "Line 1: File must start with 'WEBVTT'");
      }

      boolean seenTimestamp = false;
      long lastEndTime = -1;

      while ((line = reader.readLine()) != null) {
        lineNumber++;
        line = line.trim();
        if (line.isEmpty()) {
          continue; // blank line between cues
        }

        Matcher m = timestampPattern.matcher(line);
        if (m.matches()) {
          seenTimestamp = true;

          long start = parseTimestamp(m.group(1));
          long end = parseTimestamp(m.group(2));

          if (start >= end) {
            return new VttValidationResult(false,
                "Line " + lineNumber + ": start time must be before end time");
          }

          if (lastEndTime > start) {
            return new VttValidationResult(false,
                "Line " + lineNumber + ": cue overlaps or is out of order");
          }

          lastEndTime = end;
        }
      }

      if (!seenTimestamp) {
        return new VttValidationResult(false,
            "No valid subtitle cues found in file");
      }

      return new VttValidationResult(true, "Valid VTT file");
    } catch (IOException e) {
      return new VttValidationResult(false, "Error reading file: " + e.getMessage());
    }
  }

  public static void convert(String inputFilePath, String outputFilePath) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFilePath));
         BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {

      // Write WebVTT header
      writer.write("WEBVTT");
      writer.newLine();
      writer.newLine();

      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (matcher.matches()) {
          String start = matcher.group(1);
          String end = matcher.group(2);
          String text = matcher.group(3).trim();

          // Write timestamp
          writer.write(start + " --> " + end);
          writer.newLine();

          // Write subtitle text
          writer.write(text);
          writer.newLine();
          writer.newLine();
        }
      }
    }
  }
}
