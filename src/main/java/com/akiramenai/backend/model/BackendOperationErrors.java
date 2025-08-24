package com.akiramenai.backend.model;

public enum BackendOperationErrors {
  CourseNotFound,
  ItemNotFound,
  InvalidRequest,
  AttemptingToModifyOthersItem,
  FailedToSerializeJson,
  FailedToSaveToDb,
  FailedToSaveFile,
}
