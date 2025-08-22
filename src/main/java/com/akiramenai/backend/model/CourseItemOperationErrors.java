package com.akiramenai.backend.model;

public enum CourseItemOperationErrors {
  CourseNotFound,
  ItemNotFound,
  InvalidRequest,
  AttemptingToModifyOthersCourse,
  FailedToSerializeJson,
  FailedToSaveToDb,
  FailedToSaveFile,
}
