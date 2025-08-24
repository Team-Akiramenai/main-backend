package com.akiramenai.backend.controller;

import com.akiramenai.backend.utility.HttpResponseWriter;
import com.akiramenai.backend.utility.JsonSerializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CommentController {
  HttpResponseWriter httpResponseWriter = new HttpResponseWriter();
  JsonSerializer jsonSerializer = new JsonSerializer();

  @GetMapping("api/protected/get/comments")
  public void getComments(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam String username
  ) {
  }
}
