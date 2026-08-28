package com.example.resource_booking.service;

import com.example.resource_booking.dto.ReservationRequest;
import com.example.resource_booking.dto.ReservationResponse;
import com.example.resource_booking.exception.BadRequestException;
import com.example.resource_booking.exception.ResourceNotFoundException;
import com.example.resource_booking.model.Reservation;
import com.example.resource_booking.model.ReservationStatus;
import com.example.resource_booking.model.Resource;
import com.example.resource_booking.model.User;
import com.example.resource_booking.repository.ReservationRepository;
import com.example.resource_booking.repository.ResourceRepository;
import com.example.resource_booking.repository.UserRepository;
import com.example.resource_booking.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Dynamically derives allowed sort fields from the Reservation entity's declared fields.
     * This ensures the whitelist stays in sync with the entity without manual updates.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS;
    static {
        Set<String> fields = new java.util.HashSet<>();
        for (java.lang.reflect.Field field : Reservation.class.getDeclaredFields()) {
            fields.add(field.getName());
        }
        ALLOWED_SORT_FIELDS = java.util.Collections.unmodifiableSet(fields);
    }

    /**
     * Extracts the authenticated UserDetailsImpl from the SecurityContext.
     * Throws BadRequestException if authentication is missing or malformed.
     */
    private UserDetailsImpl getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new BadRequestException("Authentication required");
        }
        return (UserDetailsImpl) auth.getPrincipal();
    }

    private boolean isAdmin(UserDetailsImpl userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public Page<ReservationResponse> getReservations(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                                     int page, int size, String sortBy, String sortDir) {
        // Case-insensitive match of sortBy against entity fields
        String safeSortBy = ALLOWED_SORT_FIELDS.stream()
                .filter(f -> f.equalsIgnoreCase(sortBy))
                .findFirst()
                .orElse("id");
        // Sanitize sortDir to only allow 'asc' or 'desc'
        String safeSortDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";

        Sort sort = safeSortDir.equals("asc") ?
                Sort.by(safeSortBy).ascending() :
                Sort.by(safeSortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        UserDetailsImpl userDetails = getCurrentUserDetails();
        boolean adminUser = isAdmin(userDetails);

        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!adminUser) {
                predicates.add(cb.equal(root.get("user").get("id"), userDetails.getId()));
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

        return reservationRepository.findAll(spec, pageable).map(ReservationResponse::new);
    }

    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        UserDetailsImpl userDetails = getCurrentUserDetails();

        if (!isAdmin(userDetails) && !reservation.getUser().getId().equals(userDetails.getId())) {
            throw new AccessDeniedException("You do not have permission to view this reservation");
        }

        return new ReservationResponse(reservation);
    }

    public ReservationResponse createReservation(ReservationRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        UserDetailsImpl userDetails = getCurrentUserDetails();
        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        // Prevent double-booking: check for overlapping reservations for the same resource
        boolean hasOverlap = reservationRepository.existsOverlappingReservation(
                resource.getId(), request.getStartTime(), request.getEndTime());
        if (hasOverlap) {
            throw new BadRequestException("Resource is already booked for the requested time period");
        }

        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .price(request.getPrice())
                .build();

        return new ReservationResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse updateReservationStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        reservation.setStatus(status);
        return new ReservationResponse(reservationRepository.save(reservation));
    }

    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        reservationRepository.delete(reservation);
    }
}
