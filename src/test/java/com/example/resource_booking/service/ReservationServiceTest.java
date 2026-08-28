package com.example.resource_booking.service;

import com.example.resource_booking.dto.ReservationRequest;
import com.example.resource_booking.exception.BadRequestException;
import com.example.resource_booking.model.Role;
import com.example.resource_booking.model.User;
import com.example.resource_booking.repository.ReservationRepository;
import com.example.resource_booking.repository.ResourceRepository;
import com.example.resource_booking.repository.UserRepository;
import com.example.resource_booking.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReservationService reservationService;

    // Removed @BeforeEach because it causes UnnecessaryStubbingException 
    // for tests that fail before hitting the security context.

    @Test
    void createReservation_StartAfterEnd_ThrowsBadRequestException() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setPrice(new BigDecimal("15.00"));
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(1)); // End before start

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request));
    }
}
