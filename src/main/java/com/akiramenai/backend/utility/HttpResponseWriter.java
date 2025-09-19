package com.akiramenai.backend.utility;

import com.akiramenai.backend.model.BackendOperationErrors;
import com.akiramenai.backend.model.ItemId;
import com.akiramenai.backend.model.ResultOrError;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.Optional;

@Slf4j
public class HttpResponseWriter {
  private static JsonSerializer jsonSerializer = new JsonSerializer();

  public void handleDifferentResponses(
      HttpServletResponse response,
      ResultOrError<String, BackendOperationErrors> result,
      HttpStatus onSuccessHttpStatus
  ) {
    switch (result.errorType()) {
      case InvalidRequest, AttemptingToModifyOthersItem -> {
        writeFailedResponse(response, result.errorMessage(), HttpStatus.BAD_REQUEST);
        return;
      }
      case CourseNotFound, ItemNotFound -> {
        writeFailedResponse(response, result.errorMessage(), HttpStatus.NOT_FOUND);
        return;
      }
      case FailedToSaveToDb, FailedToDelete, FailedToSaveFile, FailedToSerializeJson -> {
        writeFailedResponse(response, result.errorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return;
      }
      case NotEnoughStorage -> {
        writeFailedResponse(response, result.errorMessage(), HttpStatus.INSUFFICIENT_STORAGE);
      }
      case null -> {
        writeOkResponse(response, result.result(), onSuccessHttpStatus);
        return;
      }
    }
  }

  public void writeOkResponse(
      HttpServletResponse response,
      String respJson,
      HttpStatus httpStatusCode
  ) {
    response.setContentType("application/json");
    response.setContentLength(respJson.getBytes().length);
    response.setStatus(httpStatusCode.value());

    try {
      response.getOutputStream().write(respJson.getBytes());
      response.getOutputStream().flush();
    } catch (Exception e) {
      log.error("Failed to write response. Reason: ", e);
    }
  }

  public void writeIdResponse(
      HttpServletResponse response,
      String id,
      HttpStatus responseStatus
  ) {
    ItemId itemId = new ItemId(id);
    Optional<String> respJson = jsonSerializer.serialize(itemId);
    if (respJson.isEmpty()) {
      writeFailedResponse(
          response,
          "Failed to serialize response JSON.",
          HttpStatus.INTERNAL_SERVER_ERROR
      );
      return;
    }

    writeOkResponse(response, respJson.get(), responseStatus);
  }

  public void writeFailedResponse(
      HttpServletResponse response,
      String reason,
      HttpStatus httpStatusCode
  ) {
    response.setContentType("application/text");
    response.setContentLength(reason.getBytes().length);
    response.setStatus(httpStatusCode.value());

    try {
      response.getOutputStream().write(reason.getBytes());
      response.getOutputStream().flush();
    } catch (Exception e) {
      log.error("Failed to write response. Reason: ", e);
    }
  }
}
