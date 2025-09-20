package com.akiramenai.backend.controller;

import com.akiramenai.backend.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/hls")
public class HlsController {

  private final StorageService storageService;

  public HlsController(StorageService storageService) {
    this.storageService = storageService;
  }

  @GetMapping("/{videoName}/{fileName:.+}")
  public ResponseEntity<Resource> getVideoSegment(
      @PathVariable String videoName,
      @PathVariable String fileName) throws MalformedURLException {

    videoName = videoName.substring(3);

    Path filePath = Paths.get(storageService.videoDirectoryString).resolve(videoName).resolve(fileName).normalize();
    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists()) {
      log.error("Resource not found: {}", filePath.toAbsolutePath().toString());
      return ResponseEntity.notFound().build();
    }

    String contentType;
    if (fileName.endsWith(".m3u8")) {
      contentType = "application/vnd.apple.mpegurl";
    } else if (fileName.endsWith(".ts")) {
      contentType = "video/MP2T";
    } else if (fileName.endsWith(".vtt")) {
      contentType = "text/vtt";
    } else {
      contentType = "application/octet-stream";
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, contentType)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
        .body(resource);
  }


  @GetMapping("/{videoName}/{subdir}/{fileName:.+}")
  public ResponseEntity<Resource> getVideoSegment(
      @PathVariable String videoName,
      @PathVariable String subdir,
      @PathVariable String fileName
  ) throws MalformedURLException {

    videoName = videoName.substring(3);

    Path filePath = Paths.get(storageService.videoDirectoryString).resolve(videoName).resolve(fileName).normalize();
    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists()) {
      log.error("Resource not found: {}", filePath.toAbsolutePath().toString());
      return ResponseEntity.notFound().build();
    }

    String contentType;
    if (fileName.endsWith(".m3u8")) {
      contentType = "application/vnd.apple.mpegurl";
    } else if (fileName.endsWith(".ts")) {
      contentType = "video/MP2T";
    } else if (fileName.endsWith(".vtt")) {
      contentType = "text/vtt";
    } else {
      contentType = "application/octet-stream";
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, contentType)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept")
        .body(resource);
  }
}
