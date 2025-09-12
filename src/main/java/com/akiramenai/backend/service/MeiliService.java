package com.akiramenai.backend.service;

import com.akiramenai.backend.model.Course;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.SearchResultPaginated;
import com.meilisearch.sdk.model.TaskInfo;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

  public boolean addCourseToIndex(Course course) {
    if (meiliClient == null) {
      initConnection();
    }

    JSONObject toAdd = new JSONObject()
        .put("id", course.getId())
        .put("title", course.getTitle())
        .put("description", course.getDescription())
        .put("tags", new JSONArray("[\"CS\", \"Computer Science\"]"));

    try {
      TaskInfo ti = this.coursesIndex.addDocuments(toAdd.toString(), "id");
      if (ti.getStatus().equals("canceled") || ti.getStatus().equals("failed")) {
        throw new IllegalStateException("Couldn't add course: " + course.getId());
      }
    } catch (Exception e) {
      log.error("Couldn't add course: {}", course.getId());
      return false;
    }

    return true;
  }
}
