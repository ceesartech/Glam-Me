package tech.ceesar.glamme.booking.service;

import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tech.ceesar.glamme.booking.dto.CreateBookingRequest;
import tech.ceesar.glamme.booking.dto.CreateBookingResponse;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.booking.enums.BookingStatus;
import tech.ceesar.glamme.booking.enums.PaymentStatus;
import tech.ceesar.glamme.booking.repositories.BookingRepository;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.matching.entity.ServiceOffering;
import tech.ceesar.glamme.matching.entity.StylistProfile;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    @Mock BookingRepository bookingRepo;
    @Mock ServiceOfferingRepository offeringRepo;
    @Mock PaymentService paymentService;
    @Mock CalendarService calendarService;

    @InjectMocks BookingService bookingService;

    private UUID customerId;
    private UUID stylistProfileId;
    private UUID offeringId;
    private LocalDateTime time;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        customerId       = UUID.randomUUID();
        stylistProfileId = UUID.randomUUID();
        offeringId       = UUID.randomUUID();
        time             = LocalDateTime.now().plusDays(1);
    }

    @Test
    void createBooking_success() throws StripeException {
        StylistProfile sp = StylistProfile.builder()
                .id(stylistProfileId)
                .build();

        ServiceOffering off = ServiceOffering.builder()
                .id(offeringId)
                .stylistProfile(sp)
                .build();

        when(offeringRepo.findById(offeringId))
                .thenReturn(Optional.of(off));
        when(bookingRepo.existsByStylistIdAndScheduledTime(
                stylistProfileId, time))
                .thenReturn(false);

        Booking saved = Booking.builder()
                .bookingId(UUID.randomUUID())
                .customerId(customerId)
                .stylistId(stylistProfileId)
                .offeringId(offeringId)
                .scheduledTime(time)
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .createdAt(Instant.now())
                .build();
        when(bookingRepo.save(any())).thenReturn(saved);
        when(paymentService.createCheckoutSession(saved.getBookingId()))
                .thenReturn("https://checkout.url");

        CreateBookingRequest req = new CreateBookingRequest();
        req.setCustomerId(customerId);
        req.setOfferingId(offeringId);
        req.setScheduledTime(time);

        CreateBookingResponse resp = bookingService.createBooking(req);

        assertEquals(saved.getBookingId(), resp.getBookingId());
        assertEquals("https://checkout.url", resp.getCheckoutUrl());
    }

    @Test
    void createBooking_conflict_throws() {
        StylistProfile sp = StylistProfile.builder()
                .id(stylistProfileId)
                .build();
        ServiceOffering off = ServiceOffering.builder()
                .id(offeringId)
                .stylistProfile(sp)
                .build();

        when(offeringRepo.findById(offeringId))
                .thenReturn(Optional.of(off));
        when(bookingRepo.existsByStylistIdAndScheduledTime(
                stylistProfileId, time))
                .thenReturn(true);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setCustomerId(customerId);
        req.setOfferingId(offeringId);
        req.setScheduledTime(time);

        assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(req));
    }

    @Test
    void createBooking_missingOffering_throws() {
        when(offeringRepo.findById(offeringId))
                .thenReturn(Optional.empty());

        CreateBookingRequest req = new CreateBookingRequest();
        req.setCustomerId(customerId);
        req.setOfferingId(offeringId);
        req.setScheduledTime(time);

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(req));
    }

    @Test
    void handlePaymentSucceeded_success() {
        UUID bookingId = UUID.randomUUID();
        Booking b = Booking.builder()
                .bookingId(bookingId)
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
        when(bookingRepo.findById(bookingId))
                .thenReturn(Optional.of(b));
        when(calendarService.createEvent(b))
                .thenReturn("evt-123");

        bookingService.handlePaymentSucceeded(bookingId);

        verify(bookingRepo).save(argThat(updated ->
                updated.getBookingStatus() == BookingStatus.CONFIRMED &&
                        updated.getPaymentStatus() == PaymentStatus.PAID &&
                        "evt-123".equals(updated.getCalendarEventId())
        ));
    }

    @Test
    void handlePaymentSucceeded_missingBooking_throws() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepo.findById(bookingId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.handlePaymentSucceeded(bookingId));
    }

    @Test
    void cancelBooking_success() {
        UUID bookingId = UUID.randomUUID();
        Booking b = Booking.builder()
                .bookingId(bookingId)
                .bookingStatus(BookingStatus.CONFIRMED)
                .calendarEventId("evt-123")
                .build();
        when(bookingRepo.findById(bookingId))
                .thenReturn(Optional.of(b));

        bookingService.cancelBooking(bookingId);

        verify(calendarService).deleteEvent(b);
        verify(bookingRepo).save(argThat(u ->
                u.getBookingStatus() == BookingStatus.CANCELED
        ));
    }

    @Test
    void cancelBooking_completed_throws() {
        UUID bookingId = UUID.randomUUID();
        Booking b = Booking.builder()
                .bookingId(bookingId)
                .bookingStatus(BookingStatus.COMPLETED)
                .build();
        when(bookingRepo.findById(bookingId))
                .thenReturn(Optional.of(b));

        assertThrows(BadRequestException.class,
                () -> bookingService.cancelBooking(bookingId));
    }

    @Test
    void completeBooking_success() {
        UUID bookingId = UUID.randomUUID();
        Booking b = Booking.builder()
                .bookingId(bookingId)
                .bookingStatus(BookingStatus.CONFIRMED)
                .build();
        when(bookingRepo.findById(bookingId))
                .thenReturn(Optional.of(b));

        bookingService.completeBooking(bookingId);

        verify(bookingRepo).save(argThat(u ->
                u.getBookingStatus() == BookingStatus.COMPLETED
        ));
    }

    @Test
    void completeBooking_missingBooking_throws() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepo.findById(bookingId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.completeBooking(bookingId));
    }
}
