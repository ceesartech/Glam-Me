package tech.ceesar.glamme.booking.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.booking.dto.*;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.service.BookingService;
import tech.ceesar.glamme.booking.service.PaymentService;
import tech.ceesar.glamme.booking.service.GoogleCalendarService;
import tech.ceesar.glamme.common.dto.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final GoogleCalendarService googleCalendarService;

    public BookingController(BookingService bookingService, PaymentService paymentService, GoogleCalendarService googleCalendarService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.googleCalendarService = googleCalendarService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            BookingResponse response = bookingService.createBooking(customerId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Booking created successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable String bookingId,
            @RequestParam String confirmationCode
    ) {
        try {
            BookingResponse response = bookingService.confirmBooking(bookingId, confirmationCode);
            return ResponseEntity.ok(ApiResponse.success(response, "Booking confirmed successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String bookingId,
            @RequestParam(required = false) String reason,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            BookingResponse response = bookingService.cancelBooking(bookingId, userId, reason);
            return ResponseEntity.ok(ApiResponse.success(response, "Booking cancelled successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(
            @PathVariable String bookingId,
            Authentication authentication
    ) {
        try {
            String stylistId = authentication.getName();
            BookingResponse response = bookingService.completeBooking(bookingId, stylistId);
            return ResponseEntity.ok(ApiResponse.success(response, "Booking completed successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCustomerBookings(
            Authentication authentication
    ) {
        try {
            String customerId = authentication.getName();
            List<BookingResponse> response = bookingService.getCustomerBookings(customerId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/stylist")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getStylistBookings(
            Authentication authentication
    ) {
        try {
            String stylistId = authentication.getName();
            List<BookingResponse> response = bookingService.getStylistBookings(stylistId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/availability/{stylistId}")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAvailableTimeSlots(
            @PathVariable String stylistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        try {
            List<TimeSlotResponse> response = bookingService.getAvailableTimeSlots(stylistId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Set stylist availability
     */
    @PostMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> setAvailability(
            @Valid @RequestBody AvailabilityRequest request,
            Authentication authentication
    ) {
        try {
            String stylistId = authentication.getName();
            AvailabilityResponse response = bookingService.setStylistAvailability(stylistId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Availability set successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get stylist availability
     */
    @GetMapping("/availability/{stylistId}")
    public ResponseEntity<ApiResponse<List<AvailabilityResponse>>> getAvailability(
            @PathVariable String stylistId
    ) {
        try {
            List<AvailabilityResponse> response = bookingService.getStylistAvailability(stylistId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get booking statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<BookingStats>> getBookingStats(
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            BookingStats stats = bookingService.getBookingStats(userId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reschedule booking
     */
    @PostMapping("/{bookingId}/reschedule")
    public ResponseEntity<ApiResponse<BookingResponse>> rescheduleBooking(
            @PathVariable String bookingId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newDateTime,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            BookingResponse response = bookingService.rescheduleBooking(bookingId, userId, newDateTime);
            return ResponseEntity.ok(ApiResponse.success(response, "Booking rescheduled successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get upcoming bookings for a stylist
     */
    @GetMapping("/stylist/{stylistId}/upcoming")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUpcomingStylistBookings(
            @PathVariable String stylistId,
            @RequestParam(defaultValue = "7") int daysAhead
    ) {
        try {
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = startDate.plusDays(daysAhead);
            List<BookingResponse> response = bookingService.getUpcomingBookingsForStylist(stylistId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get booking history for customer
     */
    @GetMapping("/customer/{customerId}/history")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCustomerBookingHistory(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "30") int daysBack
    ) {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(daysBack);
            List<BookingResponse> response = bookingService.getCustomerBookingHistory(customerId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get stylist's schedule for a specific date
     */
    @GetMapping("/stylist/{stylistId}/schedule")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getStylistSchedule(
            @PathVariable String stylistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date
    ) {
        try {
            List<BookingResponse> response = bookingService.getStylistScheduleForDate(stylistId, date);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Bulk update booking status
     */
    @PostMapping("/bulk/status")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> bulkUpdateStatus(
            @RequestBody BulkStatusUpdateRequest request,
            Authentication authentication
    ) {
        try {
            String userId = authentication.getName();
            List<BookingResponse> response = bookingService.bulkUpdateBookingStatus(request.getBookingIds(), request.getStatus(), userId);
            return ResponseEntity.ok(ApiResponse.success(response, "Bulk status update completed"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get booking analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<BookingAnalytics>> getBookingAnalytics(
            @RequestParam(required = false) String stylistId,
            @RequestParam(defaultValue = "30") int days
    ) {
        try {
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(days);
            BookingAnalytics analytics = bookingService.getBookingAnalytics(stylistId, startDate, endDate);
            return ResponseEntity.ok(ApiResponse.success(analytics));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create Stripe checkout session for booking payment
     */
    @PostMapping("/{bookingId}/payment/checkout")
    public ResponseEntity<ApiResponse<String>> createCheckoutSession(@PathVariable String bookingId) {
        try {
            String checkoutUrl = paymentService.createCheckoutSession(bookingId);
            return ResponseEntity.ok(ApiResponse.success(checkoutUrl, "Checkout session created successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to create checkout session: " + e.getMessage()));
        }
    }

    /**
     * Process successful payment webhook
     */
    @PostMapping("/{bookingId}/payment/success")
    public ResponseEntity<ApiResponse<String>> processPaymentSuccess(
            @PathVariable String bookingId,
            @RequestParam String paymentIntentId) {
        try {
            paymentService.processPaymentSuccess(bookingId, paymentIntentId);
            return ResponseEntity.ok(ApiResponse.success("Payment processed successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to process payment: " + e.getMessage()));
        }
    }

    /**
     * Process failed payment
     */
    @PostMapping("/{bookingId}/payment/failure")
    public ResponseEntity<ApiResponse<String>> processPaymentFailure(@PathVariable String bookingId) {
        try {
            paymentService.processPaymentFailure(bookingId);
            return ResponseEntity.ok(ApiResponse.success("Payment failure processed"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to process payment failure: " + e.getMessage()));
        }
    }

    /**
     * Create refund for booking
     */
    @PostMapping("/{bookingId}/refund")
    public ResponseEntity<ApiResponse<String>> createRefund(@PathVariable String bookingId) {
        try {
            String refundId = paymentService.createRefund(bookingId);
            return ResponseEntity.ok(ApiResponse.success(refundId, "Refund processed successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to process refund: " + e.getMessage()));
        }
    }

    /**
     * Sync booking with Google Calendar
     */
    @PostMapping("/{bookingId}/calendar/sync")
    public ResponseEntity<ApiResponse<String>> syncWithCalendar(@PathVariable String bookingId) {
        try {
            BookingResponse booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Booking not found"));
            }

            // Create a booking entity from the response for calendar sync
            Booking bookingEntity = new Booking();
            bookingEntity.setBookingId(booking.getBookingId());
            bookingEntity.setServiceName(booking.getServiceName());
            bookingEntity.setServiceDescription(booking.getServiceDescription());
            bookingEntity.setAppointmentDate(booking.getAppointmentDate());
            bookingEntity.setDurationMinutes(booking.getDurationMinutes());
            bookingEntity.setLocationAddress(booking.getLocationAddress());
            bookingEntity.setCustomerId(booking.getCustomerId());

            String eventId = googleCalendarService.createEvent(bookingEntity);
            if (eventId != null) {
                return ResponseEntity.ok(ApiResponse.success(eventId, "Calendar event created successfully"));
            } else {
                return ResponseEntity
                        .status(500)
                        .body(ApiResponse.error("Failed to create calendar event"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to sync with calendar: " + e.getMessage()));
        }
    }

    /**
     * Update calendar event for booking
     */
    @PutMapping("/{bookingId}/calendar/update")
    public ResponseEntity<ApiResponse<String>> updateCalendarEvent(@PathVariable String bookingId) {
        try {
            BookingResponse booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Booking not found"));
            }

            Booking bookingEntity = new Booking();
            bookingEntity.setBookingId(booking.getBookingId());
            bookingEntity.setServiceName(booking.getServiceName());
            bookingEntity.setServiceDescription(booking.getServiceDescription());
            bookingEntity.setAppointmentDate(booking.getAppointmentDate());
            bookingEntity.setDurationMinutes(booking.getDurationMinutes());
            bookingEntity.setLocationAddress(booking.getLocationAddress());
            bookingEntity.setCustomerId(booking.getCustomerId());
            bookingEntity.setCalendarEventId(booking.getCalendarEventId());

            String eventId = googleCalendarService.updateEvent(bookingEntity);
            if (eventId != null) {
                return ResponseEntity.ok(ApiResponse.success(eventId, "Calendar event updated successfully"));
            } else {
                return ResponseEntity
                        .status(500)
                        .body(ApiResponse.error("Failed to update calendar event"));
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to update calendar event: " + e.getMessage()));
        }
    }

    /**
     * Delete calendar event for booking
     */
    @DeleteMapping("/{bookingId}/calendar")
    public ResponseEntity<ApiResponse<String>> deleteCalendarEvent(@PathVariable String bookingId) {
        try {
            BookingResponse booking = bookingService.getBookingById(bookingId);
            if (booking == null) {
                return ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Booking not found"));
            }

            if (booking.getCalendarEventId() == null) {
                return ResponseEntity
                        .status(400)
                        .body(ApiResponse.error("No calendar event found for this booking"));
            }

            Booking bookingEntity = new Booking();
            bookingEntity.setBookingId(booking.getBookingId());
            bookingEntity.setCalendarEventId(booking.getCalendarEventId());

            googleCalendarService.deleteEvent(bookingEntity);
            return ResponseEntity.ok(ApiResponse.success("Calendar event deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error("Failed to delete calendar event: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Booking service is operational with complete functionality, payment integration, and calendar sync");
    }
}