package tech.ceesar.glamme.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.repository.BookingRepository;
import tech.ceesar.glamme.common.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing booking reminders and notifications
 */
@Service
@Slf4j
public class ReminderService {

    private final BookingRepository bookingRepository;
    private final EventService eventService;

    public ReminderService(BookingRepository bookingRepository, EventService eventService) {
        this.bookingRepository = bookingRepository;
        this.eventService = eventService;
    }

    /**
     * Send reminder for booking 24 hours before appointment
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void send24HourReminders() {
        log.info("Checking for bookings requiring 24-hour reminders");

        LocalDateTime targetTime = LocalDateTime.now().plusHours(24);
        LocalDateTime windowStart = targetTime.minusMinutes(30);
        LocalDateTime windowEnd = targetTime.plusMinutes(30);

        List<Booking> bookings = bookingRepository.findByAppointmentDateBetween(windowStart, windowEnd);

        for (Booking booking : bookings) {
            if (!booking.getReminderSent() && booking.getStatus() == Booking.Status.CONFIRMED) {
                sendReminder(booking, "24_HOUR");
                booking.setReminderSent(true);
                bookingRepository.save(booking);
            }
        }

        log.info("Sent 24-hour reminders for {} bookings", bookings.size());
    }

    /**
     * Send reminder for booking 1 hour before appointment
     */
    @Scheduled(fixedRate = 900000) // Run every 15 minutes
    public void send1HourReminders() {
        log.info("Checking for bookings requiring 1-hour reminders");

        LocalDateTime targetTime = LocalDateTime.now().plusHours(1);
        LocalDateTime windowStart = targetTime.minusMinutes(15);
        LocalDateTime windowEnd = targetTime.plusMinutes(15);

        List<Booking> bookings = bookingRepository.findByAppointmentDateBetween(windowStart, windowEnd);

        for (Booking booking : bookings) {
            if (!booking.getConfirmationSent() && booking.getStatus() == Booking.Status.CONFIRMED) {
                sendReminder(booking, "1_HOUR");
                booking.setConfirmationSent(true);
                bookingRepository.save(booking);
            }
        }

        log.info("Sent 1-hour reminders for {} bookings", bookings.size());
    }

    /**
     * Send confirmation reminder after booking is created
     */
    public void sendConfirmationReminder(Booking booking) {
        log.info("Sending confirmation reminder for booking: {}", booking.getBookingId());

        // Publish confirmation reminder event
        eventService.publishEvent("glamme-bus", "booking-service", "booking.confirmation_reminder", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "confirmationCode", booking.getConfirmationCode(),
                "reminderType", "CONFIRMATION"
        ));

        log.info("Confirmation reminder sent for booking: {}", booking.getBookingId());
    }

    /**
     * Send booking update notification
     */
    public void sendBookingUpdateNotification(Booking booking, String updateType) {
        log.info("Sending {} update notification for booking: {}", updateType, booking.getBookingId());

        eventService.publishEvent("glamme-bus", "booking-service", "booking.update_notification", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "updateType", updateType,
                "status", booking.getStatus().toString()
        ));

        log.info("Update notification sent for booking: {}", booking.getBookingId());
    }

    /**
     * Send cancellation notification
     */
    public void sendCancellationNotification(Booking booking) {
        log.info("Sending cancellation notification for booking: {}", booking.getBookingId());

        eventService.publishEvent("glamme-bus", "booking-service", "booking.cancellation_notification", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "cancellationReason", booking.getCancellationReason(),
                "cancelledAt", booking.getCancelledAt().toString()
        ));

        log.info("Cancellation notification sent for booking: {}", booking.getBookingId());
    }

    /**
     * Send completion notification
     */
    public void sendCompletionNotification(Booking booking) {
        log.info("Sending completion notification for booking: {}", booking.getBookingId());

        eventService.publishEvent("glamme-bus", "booking-service", "booking.completion_notification", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "completedAt", booking.getCompletedAt().toString(),
                "price", booking.getPrice().toString()
        ));

        log.info("Completion notification sent for booking: {}", booking.getBookingId());
    }

    /**
     * Send payment reminder for pending payments
     */
    @Scheduled(fixedRate = 21600000) // Run every 6 hours
    public void sendPaymentReminders() {
        log.info("Checking for bookings requiring payment reminders");

        // Find bookings that are pending payment and appointment is within 48 hours
        LocalDateTime cutoffTime = LocalDateTime.now().plusHours(48);

        List<Booking> pendingBookings = bookingRepository.findByAppointmentDateBetween(
                LocalDateTime.now(), cutoffTime);

        for (Booking booking : pendingBookings) {
            if (booking.getPaymentStatus() == Booking.PaymentStatus.PENDING &&
                booking.getStatus() == Booking.Status.CONFIRMED) {

                sendPaymentReminder(booking);
            }
        }

        log.info("Sent payment reminders for {} pending bookings", pendingBookings.size());
    }

    /**
     * Send payment reminder
     */
    private void sendPaymentReminder(Booking booking) {
        log.info("Sending payment reminder for booking: {}", booking.getBookingId());

        eventService.publishEvent("glamme-bus", "booking-service", "booking.payment_reminder", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "price", booking.getPrice().toString(),
                "reminderType", "PAYMENT"
        ));

        log.info("Payment reminder sent for booking: {}", booking.getBookingId());
    }

    /**
     * Send no-show notification
     */
    public void sendNoShowNotification(Booking booking) {
        log.info("Sending no-show notification for booking: {}", booking.getBookingId());

        eventService.publishEvent("glamme-bus", "booking-service", "booking.no_show_notification", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "scheduledTime", booking.getAppointmentDate().toString()
        ));

        log.info("No-show notification sent for booking: {}", booking.getBookingId());
    }

    /**
     * Send feedback request after booking completion
     */
    public void sendFeedbackRequest(Booking booking) {
        log.info("Sending feedback request for booking: {}", booking.getBookingId());

        eventService.publishEvent("glamme-bus", "booking-service", "booking.feedback_request", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "completedAt", booking.getCompletedAt().toString()
        ));

        log.info("Feedback request sent for booking: {}", booking.getBookingId());
    }

    /**
     * Generic reminder sending method
     */
    private void sendReminder(Booking booking, String reminderType) {
        log.info("Sending {} reminder for booking: {}", reminderType, booking.getBookingId());

        eventService.publishEvent("glamme-bus", "booking-service", "booking.reminder", java.util.Map.of(
                "bookingId", booking.getBookingId(),
                "customerId", booking.getCustomerId(),
                "stylistId", booking.getStylistId(),
                "appointmentDate", booking.getAppointmentDate().toString(),
                "serviceName", booking.getServiceName(),
                "reminderType", reminderType,
                "locationAddress", booking.getLocationAddress(),
                "confirmationCode", booking.getConfirmationCode()
        ));

        log.info("{} reminder sent for booking: {}", reminderType, booking.getBookingId());
    }

    /**
     * Check for expired bookings and mark as no-show
     */
    @Scheduled(fixedRate = 1800000) // Run every 30 minutes
    public void processExpiredBookings() {
        log.info("Checking for expired bookings");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);

        List<Booking> expiredBookings = bookingRepository.findByAppointmentDateBetween(
                cutoffTime.minusHours(24), cutoffTime);

        for (Booking booking : expiredBookings) {
            if (booking.getStatus() == Booking.Status.CONFIRMED &&
                booking.getAppointmentDate().isBefore(cutoffTime)) {

                booking.setStatus(Booking.Status.NO_SHOW);
                bookingRepository.save(booking);

                sendNoShowNotification(booking);

                log.info("Marked booking as no-show: {}", booking.getBookingId());
            }
        }

        log.info("Processed {} expired bookings", expiredBookings.size());
    }
}
