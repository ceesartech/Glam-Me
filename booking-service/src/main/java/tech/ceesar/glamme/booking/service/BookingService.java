package tech.ceesar.glamme.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.booking.dto.*;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.repository.BookingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ReminderService reminderService;

    public BookingService(BookingRepository bookingRepository, ReminderService reminderService) {
        this.bookingRepository = bookingRepository;
        this.reminderService = reminderService;
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

        // Send confirmation reminder
        reminderService.sendConfirmationReminder(booking);

        log.info("Successfully created booking: {}", bookingId);

        return mapToBookingResponse(booking);
    }

    public BookingResponse confirmBooking(String bookingId, String confirmationCode) {
        log.info("Confirming booking: {} with code: {}", bookingId, confirmationCode);

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getConfirmationCode().equals(confirmationCode)) {
            throw new RuntimeException("Invalid confirmation code");
        }

        if (booking.getStatus() != Booking.Status.PENDING) {
            throw new RuntimeException("Booking is not pending confirmation");
        }

        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setConfirmationSent(true);
        booking = bookingRepository.save(booking);

        // Send booking update notification
        reminderService.sendBookingUpdateNotification(booking, "CONFIRMED");

        log.info("Successfully confirmed booking: {}", bookingId);
        return mapToBookingResponse(booking);
    }

    public BookingResponse cancelBooking(String bookingId, String userId, String reason) {
        log.info("Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

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

        // Send cancellation notification
        reminderService.sendCancellationNotification(booking);

        log.info("Successfully cancelled booking: {}", bookingId);
        return mapToBookingResponse(booking);
    }

    public BookingResponse completeBooking(String bookingId, String stylistId) {
        log.info("Completing booking: {} by stylist: {}", bookingId, stylistId);

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getStylistId().equals(stylistId)) {
            throw new RuntimeException("Unauthorized to complete this booking");
        }

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new RuntimeException("Booking must be confirmed to complete");
        }

        booking.setStatus(Booking.Status.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking = bookingRepository.save(booking);

        // Send completion notification
        reminderService.sendCompletionNotification(booking);

        // Send feedback request
        reminderService.sendFeedbackRequest(booking);

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

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getCustomerId().equals(userId) && !booking.getStylistId().equals(userId)) {
            throw new RuntimeException("Unauthorized to reschedule this booking");
        }

        booking.setAppointmentDate(newDateTime);
        booking.setStatus(Booking.Status.CONFIRMED);
        booking = bookingRepository.save(booking);

        // Send booking update notification for reschedule
        reminderService.sendBookingUpdateNotification(booking, "RESCHEDULED");

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

    /**
     * Get upcoming bookings for a stylist
     */
    public List<BookingResponse> getUpcomingBookingsForStylist(String stylistId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> bookings = bookingRepository.findByStylistIdAndAppointmentDateBetween(stylistId, startDate, endDate);
        return bookings.stream()
                .filter(booking -> booking.getStatus() == Booking.Status.CONFIRMED)
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get customer booking history
     */
    public List<BookingResponse> getCustomerBookingHistory(String customerId, LocalDateTime startDate, LocalDateTime endDate) {
        // Get all customer bookings and filter by date range
        List<Booking> allBookings = bookingRepository.findByCustomerId(customerId);
        List<Booking> bookings = allBookings.stream()
                .filter(booking -> booking.getAppointmentDate().isAfter(startDate) &&
                                 booking.getAppointmentDate().isBefore(endDate))
                .filter(booking -> booking.getStatus() != Booking.Status.PENDING)
                .collect(Collectors.toList());

        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get stylist's schedule for a specific date
     */
    public List<BookingResponse> getStylistScheduleForDate(String stylistId, java.time.LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Booking> bookings = bookingRepository.findByStylistIdAndAppointmentDateBetween(stylistId, startOfDay, endOfDay);
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Bulk update booking status
     */
    public List<BookingResponse> bulkUpdateBookingStatus(List<String> bookingIds, Booking.Status newStatus, String userId) {
        List<BookingResponse> updatedBookings = new ArrayList<>();

        for (String bookingId : bookingIds) {
            try {
                Booking booking = bookingRepository.findByBookingId(bookingId)
                        .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

                // Check authorization
                if (!booking.getCustomerId().equals(userId) && !booking.getStylistId().equals(userId)) {
                    log.warn("Unauthorized attempt to update booking {} by user {}", bookingId, userId);
                    continue;
                }

                // Validate status transition
                if (!isValidStatusTransition(booking.getStatus(), newStatus)) {
                    log.warn("Invalid status transition from {} to {} for booking {}", booking.getStatus(), newStatus, bookingId);
                    continue;
                }

                booking.setStatus(newStatus);
                if (newStatus == Booking.Status.CANCELLED) {
                    booking.setCancelledAt(LocalDateTime.now());
                    booking.setCancellationReason("Bulk update by " + userId);
                }

                booking = bookingRepository.save(booking);
                updatedBookings.add(mapToBookingResponse(booking));

                log.info("Updated booking {} status to {} by user {}", bookingId, newStatus, userId);

            } catch (Exception e) {
                log.error("Failed to update booking {}: {}", bookingId, e.getMessage());
            }
        }

        return updatedBookings;
    }

    /**
     * Get booking analytics
     */
    public BookingAnalytics getBookingAnalytics(String stylistId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> allBookings;

        if (stylistId != null) {
            allBookings = bookingRepository.findByStylistId(stylistId);
        } else {
            // For simplicity, get all bookings (in production, you'd want a more efficient query)
            allBookings = bookingRepository.findAll();
        }

        // Filter by date range
        List<Booking> bookings = allBookings.stream()
                .filter(booking -> booking.getAppointmentDate().isAfter(startDate) &&
                                 booking.getAppointmentDate().isBefore(endDate))
                .collect(Collectors.toList());

        int totalBookings = bookings.size();
        int confirmedBookings = (int) bookings.stream().filter(b -> b.getStatus() == Booking.Status.CONFIRMED).count();
        int completedBookings = (int) bookings.stream().filter(b -> b.getStatus() == Booking.Status.COMPLETED).count();
        int cancelledBookings = (int) bookings.stream().filter(b -> b.getStatus() == Booking.Status.CANCELLED).count();
        int noShowBookings = (int) bookings.stream().filter(b -> b.getStatus() == Booking.Status.NO_SHOW).count();

        BigDecimal totalRevenue = bookings.stream()
                .filter(b -> b.getStatus() == Booking.Status.COMPLETED)
                .map(Booking::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBookingValue = totalBookings > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(totalBookings), 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;

        double completionRate = totalBookings > 0 ? (double) completedBookings / totalBookings * 100 : 0;
        double cancellationRate = totalBookings > 0 ? (double) cancelledBookings / totalBookings * 100 : 0;
        double noShowRate = totalBookings > 0 ? (double) noShowBookings / totalBookings * 100 : 0;

        LocalDateTime now = LocalDateTime.now();
        int upcomingBookings = (int) bookings.stream()
                .filter(b -> b.getAppointmentDate().isAfter(now) && b.getStatus() == Booking.Status.CONFIRMED)
                .count();

        int todaysBookings = (int) bookings.stream()
                .filter(b -> b.getAppointmentDate().toLocalDate().equals(now.toLocalDate()))
                .count();

        int weeklyBookings = (int) bookings.stream()
                .filter(b -> b.getAppointmentDate().isAfter(now.minusWeeks(1)))
                .count();

        int monthlyBookings = (int) bookings.stream()
                .filter(b -> b.getAppointmentDate().isAfter(now.minusMonths(1)))
                .count();

        return BookingAnalytics.builder()
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .noShowBookings(noShowBookings)
                .totalRevenue(totalRevenue)
                .averageBookingValue(averageBookingValue)
                .completionRate(completionRate)
                .cancellationRate(cancellationRate)
                .noShowRate(noShowRate)
                .upcomingBookings(upcomingBookings)
                .todaysBookings(todaysBookings)
                .weeklyBookings(weeklyBookings)
                .monthlyBookings(monthlyBookings)
                .averageRating(0.0) // Would be calculated from reviews
                .totalReviews(0) // Would be calculated from reviews
                .build();
    }

    /**
     * Validate status transition
     */
    private boolean isValidStatusTransition(Booking.Status currentStatus, Booking.Status newStatus) {
        switch (currentStatus) {
            case PENDING:
                return newStatus == Booking.Status.CONFIRMED || newStatus == Booking.Status.CANCELLED;
            case CONFIRMED:
                return newStatus == Booking.Status.COMPLETED || newStatus == Booking.Status.CANCELLED ||
                       newStatus == Booking.Status.NO_SHOW;
            case COMPLETED:
            case CANCELLED:
            case NO_SHOW:
                return false; // Terminal states
            default:
                return false;
        }
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