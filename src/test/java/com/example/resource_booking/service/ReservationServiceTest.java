package com.example.resource_booking.service;

import com.example.resource_booking.dto.ReservationRequest;
import com.example.resource_booking.dto.ReservationResponse;
import com.example.resource_booking.exception.BadRequestException;
import com.example.resource_booking.exception.ResourceNotFoundException;
import com.example.resource_booking.model.Reservation;
import com.example.resource_booking.model.ReservationStatus;
import com.example.resource_booking.model.Resource;
import com.example.resource_booking.model.Role;
import com.example.resource_booking.model.User;
import com.example.resource_booking.mapper.ReservationMapper;
import com.example.resource_booking.repository.ReservationRepository;
import com.example.resource_booking.repository.ResourceRepository;
import com.example.resource_booking.repository.UserRepository;
import com.example.resource_booking.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationService reservationService;

    private User testUser;
    private Resource testResource;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("user")
                .role(Role.USER)
                .build();

        testResource = Resource.builder()
                .id(1L)
                .name("Conference Room A")
                .type("Room")
                .build();

        testReservation = Reservation.builder()
                .id(10L)
                .user(testUser)
                .resource(testResource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(2))
                .status(ReservationStatus.PENDING)
                .price(new BigDecimal("100.00"))
                .build();

        when(reservationMapper.toResponse(any(Reservation.class)))
                .thenAnswer(invocation -> new ReservationResponse(invocation.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(User user) {
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createReservation_StartAfterEnd_ThrowsBadRequestException() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setPrice(new BigDecimal("15.00"));
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(1)); // Invalid: end before start

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void createReservation_Success() {
        mockSecurityContext(testUser);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.existsOverlappingReservation(any(), any(), any())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setPrice(new BigDecimal("100.00"));
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        ReservationResponse created = reservationService.createReservation(request);
        assertNotNull(created);
        assertEquals(ReservationStatus.PENDING, created.getStatus());
    }

    @Test
    void getReservationById_AsOwner_Success() {
        mockSecurityContext(testUser);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(testReservation));

        ReservationResponse result = reservationService.getReservationById(10L);
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void getReservationById_AsDifferentUser_ThrowsAccessDenied() {
        User otherUser = User.builder().id(2L).username("other").role(Role.USER).build();
        mockSecurityContext(otherUser);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(testReservation));

        assertThrows(AccessDeniedException.class, () -> reservationService.getReservationById(10L));
    }

    @Test
    void getReservationById_AsAdmin_Success() {
        User adminUser = User.builder().id(99L).username("admin").role(Role.ADMIN).build();
        mockSecurityContext(adminUser);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(testReservation));

        ReservationResponse result = reservationService.getReservationById(10L);
        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void updateReservationStatus_Success() {
        User adminUser = User.builder().id(99L).username("admin").role(Role.ADMIN).build();
        mockSecurityContext(adminUser);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationResponse updated = reservationService.updateReservationStatus(10L, ReservationStatus.CONFIRMED);
        assertNotNull(updated);
        assertEquals(ReservationStatus.CONFIRMED, updated.getStatus());
    }

    @Test
    void updateReservationStatus_AsNonAdmin_ThrowsAccessDenied() {
        mockSecurityContext(testUser);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(testReservation));

        assertThrows(AccessDeniedException.class,
                () -> reservationService.updateReservationStatus(10L, ReservationStatus.CONFIRMED));
        verify(reservationRepository).findById(10L);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void createReservation_WithInvalidPriceScale_ThrowsBadRequestException() {
        ReservationRequest request = new ReservationRequest();
        request.setResourceId(1L);
        request.setPrice(new BigDecimal("10.999"));
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(2));

        assertThrows(BadRequestException.class, () -> reservationService.createReservation(request));
    }

    @Test
    void getReservations_WithMinimumPriceAboveMaximumPrice_ThrowsBadRequestException() {
        mockSecurityContext(testUser);

        assertThrows(BadRequestException.class, () -> reservationService.getReservations(
                null, new BigDecimal("200.00"), new BigDecimal("100.00"),
                0, 10, "id", "asc"));
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void getReservations_WithUnsupportedSortField_ThrowsBadRequestException() {
        mockSecurityContext(testUser);

        assertThrows(BadRequestException.class, () -> reservationService.getReservations(
                null, null, null, 0, 10, "unknownField", "asc"));
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void getReservations_AppliesStatusAndPriceFilters() {
        mockSecurityContext(testUser);
        when(reservationRepository.findAll(ArgumentMatchers.<Specification<Reservation>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testReservation)));

        Page<ReservationResponse> result = reservationService.getReservations(
                ReservationStatus.PENDING, new BigDecimal("50.00"), new BigDecimal("150.00"),
                0, 10, "price", "asc");

        assertEquals(1, result.getTotalElements());
        assertEquals(ReservationStatus.PENDING, result.getContent().get(0).getStatus());
        verify(reservationRepository).findAll(
                ArgumentMatchers.<Specification<Reservation>>any(), any(Pageable.class));
    }

    @Test
    void getReservations_AppliesPaginationAndSorting() {
        mockSecurityContext(testUser);
        when(reservationRepository.findAll(ArgumentMatchers.<Specification<Reservation>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        reservationService.getReservations(null, null, null, 2, 5, "startTime", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservationRepository).findAll(
                ArgumentMatchers.<Specification<Reservation>>any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(org.springframework.data.domain.Sort.Direction.DESC,
                pageable.getSort().getOrderFor("startTime").getDirection());
    }

    @Test
    void getReservations_WithInvalidPage_ThrowsBadRequestException() {
        mockSecurityContext(testUser);

        assertThrows(BadRequestException.class, () -> reservationService.getReservations(
                null, null, null, -1, 10, "id", "asc"));
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void deleteReservation_Success() {
        User adminUser = User.builder().id(99L).username("admin").role(Role.ADMIN).build();
        mockSecurityContext(adminUser);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(testReservation));
        doNothing().when(reservationRepository).delete(testReservation);

        assertDoesNotThrow(() -> reservationService.deleteReservation(10L));
        verify(reservationRepository, times(1)).delete(testReservation);
    }

    @Test
    void deleteReservation_AsNonAdmin_ThrowsAccessDenied() {
        mockSecurityContext(testUser);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(testReservation));

        assertThrows(AccessDeniedException.class, () -> reservationService.deleteReservation(10L));
        verify(reservationRepository, never()).delete(any(Reservation.class));
    }
}
