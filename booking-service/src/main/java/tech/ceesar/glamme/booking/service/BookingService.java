package tech.ceesar.glamme.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.booking.dto.*;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public BookingResponse createBooking(String customerId, BookingRequest request) {
        log.info("Creating booking for customer: {} with stylist: {}", customerId, request.getStylistId());

        // Generate unique booking ID
        String bookingId = generateBookingId();
        String confirmationCode = generateConfirmationCode();

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .customerId(customerId)
                .stylistId(request.getStylistId())
                .serviceName(request.getServiceName())
                .serviceDescription(request.getServiceDescription())
                .appointmentDate(request.getAppointmentDate())
                .durationMinutes(request.getDurationMinutes())
                .price(request.getPrice())
                .status(Booking.Status.PENDING)
                .paymentStatus(Booking.PaymentStatus.PENDING)
                .confirmationCode(confirmationCode)
                .reminderSent(false)
                .confirmationSent(false)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Successfully created booking: {}", bookingId);

        return mapToBookingResponse(booking);
    }

    public BookingResponse confirmBooking(String bookingId, String confirmationCode) {
        log.info("Confirming booking: {} with code: {}", bookingId, confirmationCode);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found");
        }

        if (!booking.getConfirmationCode().equals(confirmationCode)) {
            throw new RuntimeException("Invalid confirmation code");
        }

        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new RuntimeException("Booking is not pending confirmation");
        }

        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setConfirmationSent(true);
        booking = bookingRepository.save(booking);

        log.info("Successfully confirmed booking: {}", bookingId);
        return mapToBookingResponse(booking);
    }

    public BookingResponse cancelBooking(String bookingId, String userId, String reason) {
        log.info("Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found");
        }

        if (!booking.getCustomerId().equals(userId) && !booking.getStylistId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this booking");
        }

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        if (booking.getStatus() == Booking.Status.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed booking");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        log.info("Successfully cancelled booking: {}", bookingId);
        return mapToBookingResponse(booking);
    }

    public BookingResponse completeBooking(String bookingId, String stylistId) {
        log.info("Completing booking: {} by stylist: {}", bookingId, stylistId);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found");
        }

        if (!booking.getStylistId().equals(stylistId)) {
            throw new RuntimeException("Unauthorized to complete this booking");
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new RuntimeException("Booking must be confirmed to complete");
        }

        booking.setStatus(Booking.Status.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        log.info("Successfully completed booking: {}", bookingId);
        return mapToBookingResponse(booking);
    }

    public List<BookingResponse> getCustomerBookings(String customerId) {
        List<Booking> bookings = bookingRepository.findByCustomerId(customerId);
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getStylistBookings(String stylistId) {
        List<Booking> bookings = bookingRepository.findByStylistId(stylistId);
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    public List<TimeSlotResponse> getAvailableTimeSlots(String stylistId, LocalDateTime startDate, LocalDateTime endDate) {
        // Simplified implementation - return empty list for now
        return List.of();
    }

    public AvailabilityResponse setStylistAvailability(String stylistId, AvailabilityRequest request) {
        log.info("Setting availability for stylist: {} - simplified implementation", stylistId);
        // Simplified implementation
        return AvailabilityResponse.builder()
                .stylistId(stylistId)
                .build();
    }

    public List<AvailabilityResponse> getStylistAvailability(String stylistId) {
        log.info("Getting availability for stylist: {} - simplified implementation", stylistId);
        // Simplified implementation
        return List.of();
    }

    // ==================== Additional Service Methods ====================

    /**
     * Reschedule booking
     */
    public BookingResponse rescheduleBooking(String bookingId, String userId, LocalDateTime newDateTime) {
        log.info("Rescheduling booking: {} by user: {} - simplified implementation", bookingId, userId);

        Booking booking = bookingRepository.findByBookingId(bookingId);
        if (booking == null) {
            throw new RuntimeException("Booking not found");
        }

        if (!booking.getCustomerId().equals(userId) && !booking.getStylistId().equals(userId)) {
            throw new RuntimeException("Unauthorized to reschedule this booking");
        }

        booking.setAppointmentDate(newDateTime);
        booking.setStatus(Booking.Status.CONFIRMED);
        booking = bookingRepository.save(booking);

        log.info("Successfully rescheduled booking: {}", bookingId);
        return mapToBookingResponse(booking);
    }

    /**
     * Get booking statistics for a user
     */
    public BookingStats getBookingStats(String userId) {
        log.info("Getting booking statistics for user: {} - simplified implementation", userId);

        return BookingStats.builder()
                .totalBookings(0)
                .pendingBookings(0)
                .confirmedBookings(0)
                .completedBookings(0)
                .cancelledBookings(0)
                .noShowBookings(0)
                .upcomingBookings(0)
                .pastBookings(0)
                .completionRate(0.0)
                .cancellationRate(0.0)
                .noShowRate(0.0)
                .build();
    }

    // ==================== Helper Methods ====================

    private String generateBookingId() {
        return "BK" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    private String generateConfirmationCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingId(booking.getBookingId())
                .customerId(booking.getCustomerId())
                .stylistId(booking.getStylistId())
                .serviceId(booking.getServiceId())
                .serviceName(booking.getServiceName())
                .serviceDescription(booking.getServiceDescription())
                .appointmentDate(booking.getAppointmentDate())
                .durationMinutes(booking.getDurationMinutes())
                .price(booking.getPrice())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .confirmationCode(booking.getConfirmationCode())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}