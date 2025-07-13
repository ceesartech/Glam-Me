package tech.ceesar.glamme.booking.service;

import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.booking.dto.CreateBookingRequest;
import tech.ceesar.glamme.booking.dto.CreateBookingResponse;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.enums.BookingStatus;
import tech.ceesar.glamme.booking.enums.PaymentStatus;
import tech.ceesar.glamme.booking.repositories.BookingRepository;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final ServiceOfferingRepository offeringRepository;
    private final PaymentService paymentService;
    private final CalendarService calendarService;

    @Transactional
    public CreateBookingResponse createBooking(CreateBookingRequest bookingRequest) throws StripeException {
        var offering = offeringRepository.findById(bookingRequest.getOfferingId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("ServiceOffering", "id", bookingRequest.getOfferingId())
                );

        if (bookingRepository.existsByStylistIdAndScheduledTime(
                offering.getStylistProfile().getId(),
                bookingRequest.getScheduledTime()
        )) {
            throw new BadRequestException("Stylist is unavailable at the requested time");
        }

        Booking booking = Booking.builder()
                .customerId(bookingRequest.getCustomerId())
                .stylistId(offering.getStylistProfile().getId())
                .offeringId(bookingRequest.getOfferingId())
                .scheduledTime(bookingRequest.getScheduledTime())
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .createdAt(Instant.now())
                .build();

        booking = bookingRepository.save(booking);
        String checkoutUrl = paymentService.createCheckoutSession(booking.getBookingId());
        return new CreateBookingResponse(booking.getBookingId(), checkoutUrl);
    }

    @Transactional
    public void cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking","id",bookingId));
        if (booking.getBookingStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed booking");
        }
        // 1) Optionally remove calendar event
        calendarService.deleteEvent(booking);
        // 2) Mark cancelled
        booking.setBookingStatus(BookingStatus.CANCELED);
        bookingRepository.save(booking);
    }

    @Transactional
    public void completeBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking","id",bookingId));
        booking.setBookingStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    @Transactional
    public void handlePaymentSucceeded(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Booking", "id", bookingId)
                );

        String eventId = calendarService.createEvent(booking);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setCalendarEventId(eventId);
        bookingRepository.save(booking);
    }
}
