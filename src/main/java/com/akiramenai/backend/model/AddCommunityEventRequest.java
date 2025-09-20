package com.akiramenai.backend.model;

public record AddCommunityEventRequest(
    String eventName,
    String eventDescription,
    String eventCoordinates,
    String eventType,

    String eventDate
) {

}
