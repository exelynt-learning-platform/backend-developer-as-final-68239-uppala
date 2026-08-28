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

    /**
     * A reservation amount is stored as DECIMAL(10,2); retaining the same limit in
     * the service prevents values that cannot be represented consistently.
     */
    private static final BigDecimal MAX_RESERVATION_PRICE = new BigDecimal("99999999.99");

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    /**
     * Explicit whitelist of valid sortable column fields on the Reservation entity.
     * Prevents runtime PropertyNotFoundException from internal/proxy properties.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "startTime", "endTime", "status", "price"
    );

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Extracts the authenticated UserDetailsImpl from the SecurityContext.
     * Throws BadRequestException if authentication is missing or malformed.
     */
    private UserDetailsImpl getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails;
        }
        throw new BadRequestException("Authentication required");
    }

    private boolean isAdmin(UserDetailsImpl userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Constructs a safe, sanitized Pageable object from raw query parameters.
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "id";
        }
        final String requestedSortBy = sortBy;

        String safeSortBy = ALLOWED_SORT_FIELDS.stream()
                .filter(field -> field.equalsIgnoreCase(requestedSortBy))
                .findFirst()
                .orElse("id");

        String safeSortDir = (sortDir != null && sortDir.equalsIgnoreCase("desc")) ? "desc" : "asc";

        Sort sort = safeSortDir.equals("asc") ?
                Sort.by(safeSortBy).ascending() :
                Sort.by(safeSortBy).descending();

        return PageRequest.of(page, size, sort);
    }

    /**
     * Builds the JPA dynamic Specification for filtering reservations by user, status, and price range.
     */
    private Specification<Reservation> buildSpecification(UserDetailsImpl userDetails,
                                                          boolean adminUser,
                                                          ReservationStatus status,
                                                          BigDecimal minPrice,
                                                          BigDecimal maxPrice) {
        return (root, query, cb) -> {
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
    }

    public Page<ReservationResponse> getReservations(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                                     int page, int size, String sortBy, String sortDir) {
        validateSearchParameters(minPrice, maxPrice, page, size);
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        UserDetailsImpl userDetails = getCurrentUserDetails();
        Specification<Reservation> spec = buildSpecification(userDetails, isAdmin(userDetails), status, minPrice, maxPrice);

        return reservationRepository.findAll(spec, pageable).map(ReservationResponse::new);
    }

    private void validateSearchParameters(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("Minimum price must not exceed maximum price");
        }
        if (page < 0) {
            throw new BadRequestException("Page number must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Page size must be between 1 and 100");
        }
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
        validateReservationPrice(request.getPrice());

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
        UserDetailsImpl userDetails = getCurrentUserDetails();
        if (!isAdmin(userDetails)) {
            throw new AccessDeniedException("Only administrators can update reservation status");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        reservation.setStatus(status);
        return new ReservationResponse(reservationRepository.save(reservation));
    }

    private void validateReservationPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new BadRequestException("Reservation price must be positive");
        }
        if (price.scale() > 2) {
            throw new BadRequestException("Reservation price can have at most two decimal places");
        }
        if (price.compareTo(MAX_RESERVATION_PRICE) > 0) {
            throw new BadRequestException("Reservation price exceeds the maximum allowed amount");
        }
    }

    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        reservationRepository.delete(reservation);
    }
}
