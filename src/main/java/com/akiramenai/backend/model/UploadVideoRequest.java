package com.akiramenai.backend.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@Builder
public class UploadVideoRequest {
  private String videoMetadataId;
  private String courseId;
  private String title;
  private String description;
}
