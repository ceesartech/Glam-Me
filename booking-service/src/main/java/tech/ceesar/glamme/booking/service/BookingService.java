package tech.ceesar.glamme.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.common.service.EventService;
import tech.ceesar.glamme.booking.dto.*;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.entity.BookingTimeSlot;
import tech.ceesar.glamme.booking.entity.StylistAvailability;
import tech.ceesar.glamme.booking.repository.BookingRepository;
import tech.ceesar.glamme.booking.repository.BookingTimeSlotRepository;
import tech.ceesar.glamme.booking.repository.StylistAvailabilityRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StylistAvailabilityRepository availabilityRepository;
    private final BookingTimeSlotRepository timeSlotRepository;
    private final EventService eventService;

    public BookingResponse createBooking(String customerId, BookingRequest request) {
        log.info("Creating booking for customer: {} with stylist: {}", customerId, request.getStylistId());

        // Validate availability
        validateBookingAvailability(request.getStylistId(), request.getAppointmentDate(), request.getDurationMinutes());

        // Generate unique booking ID
        String bookingId = generateBookingId();

        // Generate confirmation code
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
                .notes(request.getNotes())
                .specialRequests(request.getSpecialRequests())
                .addons(request.getAddons())
                .locationType(request.getLocationType())
                .locationAddress(request.getLocationAddress())
                .locationLatitude(request.getLocationLatitude())
                .locationLongitude(request.getLocationLongitude())
                .confirmationCode(confirmationCode)
                .reminderSent(false)
                .confirmationSent(false)
                .build();

        booking = bookingRepository.save(booking);

        // Create time slot
        createBookingTimeSlot(booking);

        // Publish booking created event
        eventService.publishEvent("glamme-bus", "booking-service", "booking.created", Map.of(
                "bookingId", bookingId,
                "customerId", customerId,
                "stylistId", request.getStylistId(),
                "appointmentDate", request.getAppointmentDate().toString(),
                "serviceName", request.getServiceName(),
                "price", request.getPrice().toString()
        ));

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

        // Publish booking confirmed event
        eventService.publishEvent("glamme-bus", "booking-service", "booking.confirmed", Map.of(
                "bookingId", bookingId,
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString()
        ));

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

        // Release time slot
        releaseBookingTimeSlot(booking);

        // Publish booking cancelled event
        eventService.publishEvent("glamme-bus", "booking-service", "booking.cancelled", Map.of(
                "bookingId", bookingId,
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "reason", reason,
                "cancelledBy", userId
        ));

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

        // Publish booking completed event
        eventService.publishEvent("glamme-bus", "booking-service", "booking.completed", Map.of(
                "bookingId", bookingId,
                "customerId", booking.getCustomerId(),
                "stylistId", stylistId,
                "completedAt", booking.getCompletedAt().toString()
        ));

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
        List<BookingTimeSlot> slots = timeSlotRepository.findAvailableByStylistIdAndSlotDateBetween(
                stylistId, startDate, endDate);

        return slots.stream()
                .map(this::mapToTimeSlotResponse)
                .collect(Collectors.toList());
    }

    public AvailabilityResponse setStylistAvailability(String stylistId, AvailabilityRequest request) {
        log.info("Setting availability for stylist: {}", stylistId);

        StylistAvailability availability = StylistAvailability.builder()
                .stylistId(stylistId)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isAvailable(request.getIsAvailable())
                .slotDurationMinutes(request.getSlotDurationMinutes())
                .availableServices(request.getAvailableServices())
                .maxBookingsPerSlot(request.getMaxBookingsPerSlot())
                .breakDurationMinutes(request.getBreakDurationMinutes())
                .notes(request.getNotes())
                .build();

        availability = availabilityRepository.save(availability);

        // Generate time slots for the next 30 days
        generateTimeSlots(stylistId, availability);

        log.info("Successfully set availability for stylist: {}", stylistId);
        return mapToAvailabilityResponse(availability);
    }

    public List<AvailabilityResponse> getStylistAvailability(String stylistId) {
        List<StylistAvailability> availabilities = availabilityRepository.findByStylistId(stylistId);
        return availabilities.stream()
                .map(this::mapToAvailabilityResponse)
                .collect(Collectors.toList());
    }

    private void validateBookingAvailability(String stylistId, LocalDateTime appointmentDate, Integer durationMinutes) {
        // Check if stylist has availability for this day
        StylistAvailability.DayOfWeek dayOfWeek = StylistAvailability.DayOfWeek.valueOf(
                appointmentDate.getDayOfWeek().name());

        List<StylistAvailability> availabilities = availabilityRepository.findAvailableByStylistIdAndDayOfWeek(
                stylistId, dayOfWeek);

        if (availabilities.isEmpty()) {
            throw new RuntimeException("Stylist is not available on this day");
        }

        // Check if time slot is available
        List<BookingTimeSlot> existingSlots = timeSlotRepository.findAvailableByStylistIdAndStartTime(
                stylistId, appointmentDate);

        if (!existingSlots.isEmpty()) {
            throw new RuntimeException("Time slot is already booked");
        }

        // Check if appointment is within business hours
        LocalTime appointmentTime = appointmentDate.toLocalTime();
        boolean withinBusinessHours = availabilities.stream()
                .anyMatch(avail -> appointmentTime.isAfter(avail.getStartTime()) && 
                                 appointmentTime.isBefore(avail.getEndTime()));

        if (!withinBusinessHours) {
            throw new RuntimeException("Appointment time is outside business hours");
        }
    }

    private void createBookingTimeSlot(Booking booking) {
        LocalDateTime endTime = booking.getAppointmentDate().plusMinutes(booking.getDurationMinutes());

        BookingTimeSlot timeSlot = BookingTimeSlot.builder()
                .stylistId(booking.getStylistId())
                .slotDate(booking.getAppointmentDate().toLocalDate().atStartOfDay())
                .startTime(booking.getAppointmentDate())
                .endTime(endTime)
                .isAvailable(false)
                .bookingId(booking.getId())
                .serviceName(booking.getServiceName())
                .durationMinutes(booking.getDurationMinutes())
                .build();

        timeSlotRepository.save(timeSlot);
    }

    private void releaseBookingTimeSlot(Booking booking) {
        List<BookingTimeSlot> slots = timeSlotRepository.findByStylistId(booking.getStylistId())
                .stream()
                .filter(slot -> slot.getBookingId() != null && slot.getBookingId().equals(booking.getId()))
                .collect(Collectors.toList());

        for (BookingTimeSlot slot : slots) {
            slot.setIsAvailable(true);
            slot.setBookingId(null);
            timeSlotRepository.save(slot);
        }
    }

    private void generateTimeSlots(String stylistId, StylistAvailability availability) {
        LocalDateTime startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endDate = startDate.plusDays(30);

        for (LocalDateTime date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek().name().equals(availability.getDayOfWeek().name())) {
                LocalDateTime slotStart = date.with(availability.getStartTime());
                LocalDateTime slotEnd = date.with(availability.getEndTime());

                while (slotStart.isBefore(slotEnd)) {
                    LocalDateTime slotEndTime = slotStart.plusMinutes(availability.getSlotDurationMinutes());

                    BookingTimeSlot timeSlot = BookingTimeSlot.builder()
                            .stylistId(stylistId)
                            .slotDate(date)
                            .startTime(slotStart)
                            .endTime(slotEndTime)
                            .isAvailable(true)
                            .durationMinutes(availability.getSlotDurationMinutes())
                            .build();

                    timeSlotRepository.save(timeSlot);

                    slotStart = slotEndTime;
                    if (availability.getBreakDurationMinutes() != null && availability.getBreakDurationMinutes() > 0) {
                        slotStart = slotStart.plusMinutes(availability.getBreakDurationMinutes());
                    }
                }
            }
        }
    }

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
                .paymentIntentId(booking.getPaymentIntentId())
                .notes(booking.getNotes())
                .specialRequests(booking.getSpecialRequests())
                .addons(booking.getAddons())
                .locationType(booking.getLocationType())
                .locationAddress(booking.getLocationAddress())
                .locationLatitude(booking.getLocationLatitude())
                .locationLongitude(booking.getLocationLongitude())
                .calendarEventId(booking.getCalendarEventId())
                .googleCalendarId(booking.getGoogleCalendarId())
                .appleCalendarUrl(booking.getAppleCalendarUrl())
                .confirmationCode(booking.getConfirmationCode())
                .cancellationReason(booking.getCancellationReason())
                .cancelledAt(booking.getCancelledAt())
                .completedAt(booking.getCompletedAt())
                .reminderSent(booking.getReminderSent())
                .confirmationSent(booking.getConfirmationSent())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private AvailabilityResponse mapToAvailabilityResponse(StylistAvailability availability) {
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .stylistId(availability.getStylistId())
                .dayOfWeek(availability.getDayOfWeek())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .isAvailable(availability.getIsAvailable())
                .slotDurationMinutes(availability.getSlotDurationMinutes())
                .availableServices(availability.getAvailableServices())
                .maxBookingsPerSlot(availability.getMaxBookingsPerSlot())
                .breakDurationMinutes(availability.getBreakDurationMinutes())
                .notes(availability.getNotes())
                .createdAt(availability.getCreatedAt())
                .updatedAt(availability.getUpdatedAt())
                .build();
    }

    private TimeSlotResponse mapToTimeSlotResponse(BookingTimeSlot timeSlot) {
        return TimeSlotResponse.builder()
                .id(timeSlot.getId())
                .stylistId(timeSlot.getStylistId())
                .slotDate(timeSlot.getSlotDate())
                .startTime(timeSlot.getStartTime())
                .endTime(timeSlot.getEndTime())
                .isAvailable(timeSlot.getIsAvailable())
                .bookingId(timeSlot.getBookingId())
                .serviceName(timeSlot.getServiceName())
                .durationMinutes(timeSlot.getDurationMinutes())
                .createdAt(timeSlot.getCreatedAt())
                .build();
    }
}