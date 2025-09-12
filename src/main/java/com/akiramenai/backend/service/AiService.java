package com.akiramenai.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.akiramenai.backend.model.*;

import com.akiramenai.backend.repo.VideoMetadataRepo;
import com.akiramenai.backend.utility.IdParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HarmBlockThreshold;
import com.google.genai.types.HarmCategory;
import com.google.genai.types.Part;
import com.google.genai.types.SafetySetting;

@Slf4j
@Service
public class AiService {
  private final Client gClient;
  private final String MODEL = "gemini-2.0-flash";
  private final VideoMetadataRepo videoMetadataRepo;

  @Autowired
  public AiService(Client gClient, VideoMetadataRepo videoMetadataRepo) {
    this.gClient = gClient;
    this.videoMetadataRepo = videoMetadataRepo;
  }

  public Optional<AiSuggestResponse> getCourseSuggestions(AiSuggestRequest req) throws IOException {
    if (req.question().isBlank()) {
      return Optional.empty();
    }

    List<SafetySetting> safetySettings = ImmutableList.of(
        SafetySetting.builder()
            .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
            .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
            .build(),
        SafetySetting.builder()
            .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
            .threshold(HarmBlockThreshold.Known.BLOCK_LOW_AND_ABOVE)
            .build());

    ArrayList<String> courseTitles = new ArrayList<>();
    courseTitles.add("Git Basics");
    courseTitles.add("Docker for Beginners");
    courseTitles.add("HTML for Newbies - A Gentle Introduction to the World of Web Development");
    courseTitles.add("All the CSS you'll even need - A CSS Course for Beginners and Experts Alike");

    Content systemInstruction = Content.fromParts(
        Part.fromText(
            "You are an AI assistant who provides concise and to-the-point replies. Your task is to recommend " +
                "users our courses. The replies should be in the markdown format. The following is the list of courses available in our website: "
                + courseTitles.toString()
        )
    );

    GenerateContentConfig contentConfig = GenerateContentConfig.builder()
        .candidateCount(1)
        .maxOutputTokens(1024)
        .safetySettings(safetySettings)
        .systemInstruction(systemInstruction)
        .build();

    String prompt = req.question();
    GenerateContentResponse resp = this.gClient.models.generateContent(this.MODEL, prompt, contentConfig);
    return Optional.of(new AiSuggestResponse(prompt, resp.text()));
  }

  public Optional<AiHelpResponse> getHelp(AiHelpRequest req) throws IOException {
    if (req.question().isBlank() || req.videoMetadataId().isBlank()) {
      return Optional.empty();
    }

    Optional<ParsedItemInfo> parsedItemInfo = IdParser.parseItemId(req.videoMetadataId());
    if (parsedItemInfo.isEmpty() || parsedItemInfo.get().itemType() != CourseItems.Video) {
      return Optional.empty();
    }

    Optional<VideoMetadata> videoMetadata = videoMetadataRepo.findVideoMetadataByItemId(req.videoMetadataId());
    if (videoMetadata.isEmpty()) {
      return Optional.empty();
    }

    List<SafetySetting> safetySettings = ImmutableList.of(
        SafetySetting.builder()
            .category(HarmCategory.Known.HARM_CATEGORY_HATE_SPEECH)
            .threshold(HarmBlockThreshold.Known.BLOCK_ONLY_HIGH)
            .build(),
        SafetySetting.builder()
            .category(HarmCategory.Known.HARM_CATEGORY_DANGEROUS_CONTENT)
            .threshold(HarmBlockThreshold.Known.BLOCK_LOW_AND_ABOVE)
            .build());

    Path subtitleFilePath = Paths.get(
        videoMetadata.get().getSubtitleFileName()
    );
    String subtitles;
    try {
      subtitles = Files.readString(subtitleFilePath);
    } catch (Exception e) {
      log.error("Error while reading subtitle file {}. Reason: {}", subtitleFilePath, e.toString());

      return Optional.empty();
    }

    Content systemInstruction = Content.fromParts(
        Part.fromText(
            "You are an AI assistant whose task is to help the user " +
                "by answering their questions related to a course video. The video's transcription is given below: "
                + subtitles
        )
    );

    GenerateContentConfig contentConfig = GenerateContentConfig.builder()
        .candidateCount(1)
        .maxOutputTokens(1024)
        .safetySettings(safetySettings)
        .systemInstruction(systemInstruction)
        .build();

    String prompt = req.question();
    GenerateContentResponse resp = this.gClient.models.generateContent(this.MODEL, prompt, contentConfig);
    return Optional.of(new AiHelpResponse(resp.text()));
  }
}
