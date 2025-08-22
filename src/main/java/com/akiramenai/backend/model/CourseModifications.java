package com.akiramenai.backend.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CourseModifications {
  private UUID id;
  private String title;
  private String description;
  private Double price;
}
