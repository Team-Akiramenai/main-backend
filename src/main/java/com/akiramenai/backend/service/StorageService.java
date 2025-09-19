package com.akiramenai.backend.service;

import com.akiramenai.backend.model.FileUploadErrorTypes;
import com.akiramenai.backend.model.ResultOrError;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@Service
public class StorageService {
  @Value("${application.default-values.media.picture-directory}")
  public String pictureDirectoryString;

  @Value("${application.default-values.media.subtitles-directory}")
  public String subtitlesDirectoryString;

  @Value("${application.default-values.scripts.script-storage-directory}")
  public String scriptDirectoryString;

  public static MediaType getFileType(MultipartFile file) {
    if (file.getContentType() == null) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
    return MediaType.parseMediaType(file.getContentType());
  }

  public static MediaType getFileType(Path filepath) {
    try {
      String mimeType = Files.probeContentType(filepath);
      return MediaType.parseMediaType(mimeType);
    } catch (Exception e) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  public ResultOrError<String, FileUploadErrorTypes> saveImage(MultipartFile file) {
    var resp = ResultOrError.<String, FileUploadErrorTypes>builder();
    if (file.isEmpty()) {
      return resp
          .errorMessage("Uploaded file is empty.")
          .errorType(FileUploadErrorTypes.FileIsEmpty)
          .result(null)
          .build();
    }
    if ((!getFileType(file).equals(MediaType.IMAGE_PNG)) && (!getFileType(file).equals(MediaType.IMAGE_JPEG))) {
      return resp
          .errorMessage("Unsupported image file format. We only support PNG and JPEG.")
          .errorType(FileUploadErrorTypes.UnsupportedFileType)
          .build();
    }

    try {
      Path uploadPath = Paths.get(this.pictureDirectoryString);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String fileName = getGeneratedFileName(file.getOriginalFilename());
      Path filePath = uploadPath.resolve(fileName);
      Files.copy(file.getInputStream(), filePath);

      return ResultOrError
          .<String, FileUploadErrorTypes>builder()
          .result(filePath.getFileName().toString())
          .errorMessage(null)
          .errorType(null)
          .build();
    } catch (InvalidPathException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.InvalidUploadDir)
          .result(null)
          .build();
    } catch (UnsupportedOperationException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.FailedToCreateUploadDir)
          .result(null)
          .build();
    } catch (IOException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.FailedToSaveFile)
          .result(null)
          .build();
    }
  }

  public ResultOrError<Path, FileUploadErrorTypes> saveVtt(MultipartFile file) {
    var resp = ResultOrError.<Path, FileUploadErrorTypes>builder();
    if (file.isEmpty()) {
      return resp
          .errorMessage("Uploaded file is empty.")
          .errorType(FileUploadErrorTypes.FileIsEmpty)
          .result(null)
          .build();
    }
    if (file.getContentType() == null || !file.getContentType().equals("text/vtt")) {
      return resp
          .errorMessage("Invalid content type. Only text/vtt content type is accepted.")
          .errorType(FileUploadErrorTypes.UnsupportedFileType)
          .build();
    }

    try {
      Path uploadPath = Paths.get(this.subtitlesDirectoryString);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String fileName = getGeneratedFileName(file.getOriginalFilename());
      Path filePath = uploadPath.resolve(fileName);
      Files.copy(file.getInputStream(), filePath);

      return resp
          .result(filePath)
          .errorMessage(null)
          .errorType(null)
          .build();
    } catch (InvalidPathException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.InvalidUploadDir)
          .result(null)
          .build();
    } catch (UnsupportedOperationException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.FailedToCreateUploadDir)
          .result(null)
          .build();
    } catch (IOException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.FailedToSaveFile)
          .result(null)
          .build();
    }
  }

  public ResultOrError<Path, FileUploadErrorTypes> saveScript(MultipartFile file) {
    var resp = ResultOrError.<Path, FileUploadErrorTypes>builder();
    if (file.isEmpty()) {
      return resp
          .errorMessage("Failed to save script file. Provided script file is empty.")
          .errorType(FileUploadErrorTypes.FileIsEmpty)
          .build();
    }

    try {
      Path uploadPath = Paths.get(this.scriptDirectoryString);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String fileName = getGeneratedFileName(file.getOriginalFilename());
      Path filePath = uploadPath.resolve(fileName);
      Files.copy(file.getInputStream(), filePath);

      return resp
          .result(filePath)
          .errorMessage(null)
          .errorType(null)
          .build();
    } catch (InvalidPathException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.InvalidUploadDir)
          .result(null)
          .build();
    } catch (UnsupportedOperationException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.FailedToCreateUploadDir)
          .result(null)
          .build();
    } catch (IOException e) {
      return resp
          .errorMessage(e.getMessage())
          .errorType(FileUploadErrorTypes.FailedToSaveFile)
          .result(null)
          .build();
    }
  }

  private String getFileExtension(@NotNull String filename) {
    int dotIndex = filename.lastIndexOf(".");
    if (dotIndex >= 0) {
      return filename.substring(dotIndex + 1);
    }

    return "";
  }

  private String getGeneratedFileName(String filename) {
    return UUID.randomUUID().toString() + "." + getFileExtension(filename);
  }
}
