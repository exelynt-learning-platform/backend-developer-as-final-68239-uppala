package com.example.resource_booking.mapper;

import com.example.resource_booking.dto.ReservationResponse;
import com.example.resource_booking.model.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation);
    }
}
