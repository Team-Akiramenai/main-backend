package com.akiramenai.backend;

import com.akiramenai.backend.controller.CourseController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class BackendMainTests {

  @Autowired
  private CourseController courseController;

  @Test
  void contextLoads() throws Exception {
    assertThat(courseController).isNotNull();
  }

}
