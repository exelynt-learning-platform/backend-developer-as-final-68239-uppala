package com.example.resource_booking.service;

import com.example.resource_booking.dto.ReservationRequest;
import com.example.resource_booking.dto.ReservationResponse;
import com.example.resource_booking.exception.BadRequestException;
import com.example.resource_booking.exception.ResourceNotFoundException;
import com.example.resource_booking.model.Reservation;
import com.example.resource_booking.model.ReservationStatus;
import com.example.resource_booking.model.Resource;
import com.example.resource_booking.model.User;
import com.example.resource_booking.mapper.ReservationMapper;
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ReservationService {

    public static final String FIELD_ID = "id";
    public static final String FIELD_USER = "user";
    public static final String FIELD_RESOURCE = "resource";
    public static final String FIELD_START_TIME = "startTime";
    public static final String FIELD_END_TIME = "endTime";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_PRICE = "price";

    /**
     * A reservation amount is stored as DECIMAL(10,2); retaining the same limit in
     * the service prevents values that cannot be represented consistently.
     */
    private static final BigDecimal MAX_RESERVATION_PRICE = new BigDecimal("99999999.99");

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final ReservationMapper reservationMapper;

    /**
     * Explicit whitelist of valid sortable column fields on the Reservation entity.
     * Prevents runtime PropertyNotFoundException from internal/proxy properties.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            FIELD_ID, FIELD_START_TIME, FIELD_END_TIME, FIELD_STATUS, FIELD_PRICE
    );

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository,
                              ReservationMapper reservationMapper) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.reservationMapper = reservationMapper;
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
        String safeSortBy = ALLOWED_SORT_FIELDS.stream()
                .filter(field -> field.equalsIgnoreCase(sortBy))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Unsupported sort field. Allowed values: " + String.join(", ", ALLOWED_SORT_FIELDS)));

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
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = buildPredicates(
                    root, criteriaBuilder, userDetails, adminUser, status, minPrice, maxPrice);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<Predicate> buildPredicates(Root<Reservation> root,
                                            CriteriaBuilder criteriaBuilder,
                                            UserDetailsImpl userDetails,
                                            boolean adminUser,
                                            ReservationStatus status,
                                            BigDecimal minPrice,
                                            BigDecimal maxPrice) {
        List<Predicate> predicates = new ArrayList<>();

        if (!adminUser) {
            predicates.add(criteriaBuilder.equal(root.get(FIELD_USER).get(FIELD_ID), userDetails.getId()));
        }
        if (status != null) {
            predicates.add(criteriaBuilder.equal(root.get(FIELD_STATUS), status));
        }
        if (minPrice != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(FIELD_PRICE), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(FIELD_PRICE), maxPrice));
        }

        return predicates;
    }

    public Page<ReservationResponse> getReservations(ReservationStatus status, BigDecimal minPrice, BigDecimal maxPrice,
                                                     int page, int size, String sortBy, String sortDir) {
        validateSearchParameters(minPrice, maxPrice, page, size);
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        UserDetailsImpl userDetails = getCurrentUserDetails();
        Specification<Reservation> spec = buildSpecification(userDetails, isAdmin(userDetails), status, minPrice, maxPrice);

        return reservationRepository.findAll(spec, pageable).map(reservationMapper::toResponse);
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

        if (!canAccessReservation(reservation)) {
            throw new AccessDeniedException("You do not have permission to view this reservation");
        }

        return reservationMapper.toResponse(reservation);
    }

    public ReservationResponse createReservation(ReservationRequest request) {
        validateReservationRequest(request);

        UserDetailsImpl userDetails = getCurrentUserDetails();
        User currentUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + request.getResourceId()));

        ensureNoOverlappingReservation(resource.getId(), request.getStartTime(), request.getEndTime());

        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .price(request.getPrice())
                .build();

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    private void validateReservationRequest(ReservationRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Start time and end time are required");
        }
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }
        validateReservationPrice(request.getPrice());
    }

    private void ensureNoOverlappingReservation(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean hasOverlap = reservationRepository.existsOverlappingReservation(resourceId, startTime, endTime);
        if (hasOverlap) {
            throw new BadRequestException("Resource is already booked for the requested time period");
        }
    }

    public ReservationResponse updateReservationStatus(Long id, ReservationStatus status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
        requireAdmin();
        reservation.setStatus(status);
        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    private void requireAdmin() {
        if (!isAdmin(getCurrentUserDetails())) {
            throw new AccessDeniedException("Only administrators can update reservation status");
        }
    }

    private boolean canAccessReservation(Reservation reservation) {
        UserDetailsImpl userDetails = getCurrentUserDetails();
        return isAdmin(userDetails) || reservation.getUser().getId().equals(userDetails.getId());
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
        requireAdmin();
        reservationRepository.delete(reservation);
    }
}
