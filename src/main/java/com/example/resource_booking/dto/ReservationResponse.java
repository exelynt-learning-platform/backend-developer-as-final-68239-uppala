package com.example.resource_booking.dto;

import com.example.resource_booking.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Reservation API responses.
 * Avoids exposing JPA entity internals, lazy-loading proxies, and sensitive user data.
 */
public class ReservationResponse {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private Long userId;
    private String username;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;
    private BigDecimal price;

    public ReservationResponse() {}

    public ReservationResponse(com.example.resource_booking.model.Reservation reservation) {
        this.id = reservation.getId();
        this.resourceId = reservation.getResource() != null ? reservation.getResource().getId() : null;
        this.resourceName = reservation.getResource() != null ? reservation.getResource().getName() : null;
        this.userId = reservation.getUser() != null ? reservation.getUser().getId() : null;
        this.username = reservation.getUser() != null ? reservation.getUser().getUsername() : null;
        this.startTime = reservation.getStartTime();
        this.endTime = reservation.getEndTime();
        this.status = reservation.getStatus();
        this.price = reservation.getPrice();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
