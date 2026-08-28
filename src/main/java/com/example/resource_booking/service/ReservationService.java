package com.example.resource_booking.service;

import com.example.resource_booking.dto.ReservationRequest;
import com.example.resource_booking.exception.BadRequestException;
import com.example.resource_booking.exception.ResourceNotFoundException;
import com.example.resource_booking.model.Reservation;
import com.example.resource_booking.model.ReservationStatus;
import com.example.resource_booking.model.Resource;
import com.example.resource_booking.model.Role;
import com.example.resource_booking.model.User;
import com.example.resource_booking.repository.ReservationRepository;
import com.example.resource_booking.repository.ResourceRepository;
import com.example.resource_booking.repository.UserRepository;
import com.example.resource_booking.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    public Page<Reservation> getReservations(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (currentUser.getRole() == Role.USER) {
                predicates.add(cb.equal(root.get("user").get("id"), currentUser.getId()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reservationRepository.findAll(spec, pageable);
    }

    public Reservation getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() == Role.USER && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this reservation");
        }

        return reservation;
    }

    public Reservation createReservation(ReservationRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .price(request.getPrice())
                .build();

        return reservationRepository.save(reservation);
    }

    public Reservation updateReservationStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        reservation.setStatus(status);
        return reservationRepository.save(reservation);
    }

    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        reservationRepository.delete(reservation);
    }
}
