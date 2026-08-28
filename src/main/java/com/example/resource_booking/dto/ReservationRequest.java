package com.example.resource_booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequest {
    @NotNull
    private Long resourceId;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;

    @NotNull
    @jakarta.validation.constraints.Positive
    private java.math.BigDecimal price;
}
