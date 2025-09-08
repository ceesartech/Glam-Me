package tech.ceesar.glamme.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.ceesar.glamme.booking.dto.*;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.repository.BookingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ReminderService reminderService;

    @InjectMocks
    private BookingService bookingService;

    private String customerId;
    private String stylistId;
    private BookingRequest bookingRequest;
    private Booking sampleBooking;
    private LocalDateTime appointmentDate;

    @BeforeEach
    void setUp() {
        customerId = "customer-123";
        stylistId = "stylist-456";
        appointmentDate = LocalDateTime.now().plusDays(1);

        bookingRequest = BookingRequest.builder()
                .stylistId(stylistId)
                .serviceName("Haircut")
                .serviceDescription("Professional haircut and styling")
                .appointmentDate(appointmentDate)
                .durationMinutes(60)
                .price(BigDecimal.valueOf(75.00))
                .build();

        sampleBooking = Booking.builder()
                .bookingId("booking-123")
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .serviceDescription("Professional haircut and styling")
                .appointmentDate(appointmentDate)
                .durationMinutes(60)
                .price(BigDecimal.valueOf(75.00))
                .status(Booking.Status.PENDING)
                .paymentStatus(Booking.PaymentStatus.PENDING)
                .confirmationCode("CONF123")
                .reminderSent(false)
                .confirmationSent(false)
                .build();
    }

    @Test
    void createBooking_Success() {
        // Arrange
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        // Act
        BookingResponse result = bookingService.createBooking(customerId, bookingRequest);

        // Assert
        assertNotNull(result);
        assertEquals(sampleBooking.getBookingId(), result.getBookingId());
        assertEquals(customerId, result.getCustomerId());
        assertEquals(stylistId, result.getStylistId());
        assertEquals("Haircut", result.getServiceName());
        assertEquals(Booking.Status.PENDING, result.getStatus());
        assertEquals(Booking.PaymentStatus.PENDING, result.getPaymentStatus());

        verify(bookingRepository).save(any(Booking.class));
        verify(reminderService).sendConfirmationReminder(any(Booking.class));
    }

    @Test
    void createBooking_WithNullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
            bookingService.createBooking(customerId, null));
        
        verify(bookingRepository, never()).save(any());
        verify(reminderService, never()).sendConfirmationReminder(any());
    }

    @Test
    void confirmBooking_Success() {
        // Arrange
        String bookingId = "booking-123";
        String confirmationCode = "123456";
        
        Booking pendingBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(appointmentDate)
                .status(Booking.Status.PENDING)
                .paymentStatus(Booking.PaymentStatus.PENDING)
                .confirmationCode(confirmationCode)
                .build();

        Booking confirmedBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(appointmentDate)
                .status(Booking.Status.CONFIRMED)
                .paymentStatus(Booking.PaymentStatus.CAPTURED)
                .confirmationCode(confirmationCode)
                .build();

        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);

        // Act
        BookingResponse result = bookingService.confirmBooking(bookingId, confirmationCode);

        // Assert
        assertNotNull(result);
        assertEquals(Booking.Status.CONFIRMED, result.getStatus());

        verify(bookingRepository).findByBookingId(bookingId);
        verify(bookingRepository).save(any(Booking.class));
        verify(reminderService).sendBookingUpdateNotification(any(Booking.class), eq("CONFIRMED"));
    }

    @Test
    void confirmBooking_BookingNotFound_ThrowsException() {
        // Arrange
        String bookingId = "non-existent";
        String confirmationCode = "123456";
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            bookingService.confirmBooking(bookingId, confirmationCode));
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmBooking_InvalidConfirmationCode_ThrowsException() {
        // Arrange
        String bookingId = "booking-123";
        String wrongCode = "wrong-code";
        
        Booking bookingWithCode = sampleBooking.toBuilder()
                .confirmationCode("123456")
                .build();
        
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(bookingWithCode));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            bookingService.confirmBooking(bookingId, wrongCode));
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_Success() {
        // Arrange
        String bookingId = "booking-123";
        String reason = "Schedule conflict";
        
        Booking confirmedBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(appointmentDate)
                .status(Booking.Status.CONFIRMED)
                .build();
        
        Booking cancelledBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(appointmentDate)
                .status(Booking.Status.CANCELLED)
                .cancellationReason(reason)
                .cancelledAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(cancelledBooking);

        // Act
        BookingResponse result = bookingService.cancelBooking(bookingId, customerId, reason);

        // Assert
        assertNotNull(result);
        assertEquals(Booking.Status.CANCELLED, result.getStatus());

        verify(bookingRepository).findByBookingId(bookingId);
        verify(bookingRepository).save(any(Booking.class));
        verify(reminderService).sendCancellationNotification(any(Booking.class));
    }

    @Test
    void cancelBooking_AlreadyCancelled_ThrowsException() {
        // Arrange
        String bookingId = "booking-123";
        String reason = "Schedule conflict";
        
        Booking cancelledBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .status(Booking.Status.CANCELLED)
                .build();

        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(cancelledBooking));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            bookingService.cancelBooking(bookingId, customerId, reason));
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void completeBooking_Success() {
        // Arrange
        String bookingId = "booking-123";
        
        Booking confirmedBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(appointmentDate)
                .status(Booking.Status.CONFIRMED)
                .build();
        
        Booking completedBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(appointmentDate)
                .status(Booking.Status.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(completedBooking);

        // Act
        BookingResponse result = bookingService.completeBooking(bookingId, stylistId);

        // Assert
        assertNotNull(result);
        assertEquals(Booking.Status.COMPLETED, result.getStatus());

        verify(bookingRepository).findByBookingId(bookingId);
        verify(bookingRepository).save(any(Booking.class));
        verify(reminderService).sendCompletionNotification(any(Booking.class));
        verify(reminderService).sendFeedbackRequest(any(Booking.class));
    }

    @Test
    void completeBooking_UnauthorizedStylist_ThrowsException() {
        // Arrange
        String bookingId = "booking-123";
        String wrongStylistId = "wrong-stylist";
        
        Booking confirmedBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .status(Booking.Status.CONFIRMED)
                .build();

        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(confirmedBooking));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            bookingService.completeBooking(bookingId, wrongStylistId));
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getCustomerBookings_Success() {
        // Arrange
        List<Booking> bookings = Arrays.asList(
            sampleBooking,
            Booking.builder()
                .bookingId("booking-456")
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Hair Coloring")
                .appointmentDate(appointmentDate.plusDays(1))
                .status(Booking.Status.CONFIRMED)
                .build()
        );

        when(bookingRepository.findByCustomerId(customerId)).thenReturn(bookings);

        // Act
        List<BookingResponse> result = bookingService.getCustomerBookings(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("booking-123", result.get(0).getBookingId());
        assertEquals("booking-456", result.get(1).getBookingId());
        assertEquals("Haircut", result.get(0).getServiceName());
        assertEquals("Hair Coloring", result.get(1).getServiceName());

        verify(bookingRepository).findByCustomerId(customerId);
    }

    @Test
    void getStylistBookings_Success() {
        // Arrange
        List<Booking> bookings = Arrays.asList(
            sampleBooking,
            Booking.builder()
                .bookingId("booking-789")
                .customerId("customer-456")
                .stylistId(stylistId)
                .serviceName("Styling")
                .appointmentDate(appointmentDate.plusDays(2))
                .status(Booking.Status.COMPLETED)
                .build()
        );

        when(bookingRepository.findByStylistId(stylistId)).thenReturn(bookings);

        // Act
        List<BookingResponse> result = bookingService.getStylistBookings(stylistId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(stylistId, result.get(0).getStylistId());
        assertEquals(stylistId, result.get(1).getStylistId());

        verify(bookingRepository).findByStylistId(stylistId);
    }

    @Test
    void getBookingStats_Success() {
        // Arrange
        String userId = "user-123";

        // Act
        BookingStats result = bookingService.getBookingStats(userId);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalBookings());
        assertEquals(0, result.getPendingBookings());
        assertEquals(0, result.getConfirmedBookings());
        assertEquals(0, result.getCompletedBookings());
        assertEquals(0, result.getCancelledBookings());
        assertEquals(0.0, result.getCompletionRate());
        assertEquals(0.0, result.getCancellationRate());
    }

    @Test
    void getUpcomingBookingsForStylist_Success() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(7);
        
        List<Booking> bookings = Arrays.asList(
            Booking.builder()
                .bookingId("booking-1")
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(startDate.plusDays(1))
                .status(Booking.Status.CONFIRMED)
                .build(),
            Booking.builder()
                .bookingId("booking-2")
                .customerId("customer-456")
                .stylistId(stylistId)
                .serviceName("Styling")
                .appointmentDate(startDate.plusDays(3))
                .status(Booking.Status.CONFIRMED)
                .build()
        );

        when(bookingRepository.findByStylistIdAndAppointmentDateBetween(stylistId, startDate, endDate))
                .thenReturn(bookings);

        // Act
        List<BookingResponse> result = bookingService.getUpcomingBookingsForStylist(stylistId, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(b -> b.getStatus() == Booking.Status.CONFIRMED));

        verify(bookingRepository).findByStylistIdAndAppointmentDateBetween(stylistId, startDate, endDate);
    }

    @Test
    void getCustomerBookingHistory_Success() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        LocalDateTime endDate = LocalDateTime.now();
        
        List<Booking> allBookings = Arrays.asList(
            Booking.builder()
                .bookingId("booking-1")
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(startDate.plusDays(5))
                .status(Booking.Status.COMPLETED)
                .build(),
            Booking.builder()
                .bookingId("booking-2")
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Styling")
                .appointmentDate(startDate.plusDays(10))
                .status(Booking.Status.CANCELLED)
                .build(),
            Booking.builder()
                .bookingId("booking-3")
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Treatment")
                .appointmentDate(startDate.plusDays(15))
                .status(Booking.Status.PENDING)
                .build()
        );

        when(bookingRepository.findByCustomerId(customerId)).thenReturn(allBookings);

        // Act
        List<BookingResponse> result = bookingService.getCustomerBookingHistory(customerId, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Should exclude PENDING status
        assertTrue(result.stream().noneMatch(b -> b.getStatus() == Booking.Status.PENDING));

        verify(bookingRepository).findByCustomerId(customerId);
    }

    @Test
    void getAvailableTimeSlots_ReturnsEmptyList() {
        // Arrange
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(1);

        // Act
        List<TimeSlotResponse> result = bookingService.getAvailableTimeSlots(stylistId, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Current implementation returns empty list
    }

    @Test
    void rescheduleBooking_Success() {
        // Arrange
        String bookingId = "booking-123";
        LocalDateTime newDateTime = appointmentDate.plusDays(1);
        
        Booking existingBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(appointmentDate)
                .status(Booking.Status.CONFIRMED)
                .build();
        
        Booking rescheduledBooking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(stylistId)
                .serviceName("Haircut")
                .appointmentDate(newDateTime)
                .status(Booking.Status.CONFIRMED)
                .build();

        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(existingBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(rescheduledBooking);

        // Act
        BookingResponse result = bookingService.rescheduleBooking(bookingId, customerId, newDateTime);

        // Assert
        assertNotNull(result);
        assertEquals(newDateTime, result.getAppointmentDate());
        assertEquals(Booking.Status.CONFIRMED, result.getStatus());

        verify(bookingRepository).findByBookingId(bookingId);
        verify(bookingRepository).save(any(Booking.class));
        verify(reminderService).sendBookingUpdateNotification(any(Booking.class), eq("RESCHEDULED"));
    }

    @Test
    void rescheduleBooking_UnauthorizedUser_ThrowsException() {
        // Arrange
        String bookingId = "booking-123";
        String wrongUserId = "wrong-user";
        LocalDateTime newDateTime = appointmentDate.plusDays(1);
        
        when(bookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(sampleBooking));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            bookingService.rescheduleBooking(bookingId, wrongUserId, newDateTime));
        
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rescheduleBooking_WrongUser_ThrowsException() {
        // Arrange
        UUID bookingId = UUID.randomUUID();
        String wrongUserId = "wrong-user";
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(1);
        Booking sampleBooking = Booking.builder()
                .bookingId(bookingId.toString())
                .customerId(UUID.randomUUID().toString())
                .build();

        when(bookingRepository.findByBookingId(bookingId.toString())).thenReturn(Optional.of(sampleBooking));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
            bookingService.rescheduleBooking(bookingId.toString(), wrongUserId, newDateTime));

        verify(bookingRepository, never()).save(any());
    }
}