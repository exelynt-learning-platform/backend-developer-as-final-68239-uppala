package com.example.resource_booking.repository;

import com.example.resource_booking.model.Reservation;
import com.example.resource_booking.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    /**
     * Detects if any active (non-CANCELLED) reservation already occupies the requested time slot
     * for the given resource — preventing double-bookings.
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.resource.id = :resourceId " +
           "AND r.status <> com.example.resource_booking.model.ReservationStatus.CANCELLED " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    boolean existsOverlappingReservation(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}

