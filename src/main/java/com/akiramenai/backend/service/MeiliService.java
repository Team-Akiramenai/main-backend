package com.akiramenai.backend.service;

import com.akiramenai.backend.model.Course;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.SearchResultPaginated;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class MeiliService {
  @Value("${application.meili-search.master-key}")
  private String meiliSearchMasterKey;

  private Client meiliClient = null;
  private Index coursesIndex = null;

  public MeiliService() {
  }

  private void initConnection() {
    this.meiliClient = new Client(
        new Config(
            "http://localhost:7700",
            meiliSearchMasterKey
        )
    );

    this.coursesIndex = meiliClient.index("courses");
  }

  private JSONObject getExtractedCourseJson(Course course, @Nullable String instructorName) {
    double rating = course.getTotalStars() / (double) course.getUsersWhoRatedCount();
    String truncatedRating = (course.getUsersWhoRatedCount() == 0) ? "0.0" : String.format("%.2f", rating);

    JSONObject toAdd = new JSONObject()
        .put("id", course.getId())
        .put("title", course.getTitle())
        .put("description", course.getDescription())
        .put("thumbnailImageName", course.getThumbnailImageName())
        .put("price", course.getPrice())
        .put("rating", truncatedRating)
        .put("voterCount", course.getUsersWhoRatedCount())
        .put("createdAt", course.getCreatedAt())
        .put("lastModifiedAt", course.getLastModifiedAt())
        .put("tags", new JSONArray(course.getTags()));

    if (instructorName != null) {
      toAdd.put("instructor", instructorName);
    }

    return toAdd;
  }

  public Optional<SearchResultPaginated> searchCourses(String query, int pageNumber, int pageSize) {
    if (meiliClient == null) {
      initConnection();
    }

    try {
      SearchResultPaginated results = (SearchResultPaginated) coursesIndex.search(
          new SearchRequest(query)
              .setPage(pageNumber)
              .setHitsPerPage(pageSize)
      );
      return Optional.of(results);
    } catch (Exception e) {
      log.error("Failed to search courses. Reason: {}", e.toString());
      return Optional.empty();
    }
  }

  public boolean addCourseToIndex(Course course, String instructorName) {
    if (meiliClient == null) {
      initConnection();
    }

    JSONObject courseJsonToAdd = getExtractedCourseJson(course, instructorName);

    try {
      this.coursesIndex.addDocuments(courseJsonToAdd.toString(), "id");
    } catch (Exception e) {
      log.error("Couldn't add course: {}", course.getId());
      return false;
    }

    return true;
  }

  public boolean updateCourseInDocument(Course course) {
    if (meiliClient == null) {
      initConnection();
    }

    JSONObject courseJsonToUpdate = getExtractedCourseJson(course, null);

    try {
      this.coursesIndex.updateDocuments(courseJsonToUpdate.toString(), "id");
    } catch (Exception e) {
      log.error("Couldn't update course: {}", course.getId());
      return false;
    }

    return true;
  }

  public boolean deleteCourseInDocument(String courseId) {
    if (meiliClient == null) {
      initConnection();
    }

    try {
      this.coursesIndex.deleteDocument(courseId);
    } catch (Exception e) {
      log.error("Couldn't delete course: {}", courseId);
      return false;
    }

    return true;
  }
}
