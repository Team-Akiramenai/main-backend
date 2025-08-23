package com.akiramenai.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ModifyVideoMetadataRequest {
  private String itemId;
  private String courseId;
  private String title;
  private String description;
}
