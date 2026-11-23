package com.ruben.backend.participation.dto;

/** How many people joined one trip; the result of counting the participations of many trips. */
public record TripParticipantCount(Long tripId, long count) {}
