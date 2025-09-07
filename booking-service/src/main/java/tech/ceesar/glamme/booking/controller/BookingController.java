package tech.ceesar.glamme.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.booking.dto.*;
import tech.ceesar.glamme.common.dto.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

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
}