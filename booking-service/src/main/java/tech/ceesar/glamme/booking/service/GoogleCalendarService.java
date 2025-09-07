package tech.ceesar.glamme.booking.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.booking.entity.Booking;

import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService implements CalendarService {

    private final Calendar calendar;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Override
    public String createEvent(Booking booking) {
        log.info("Creating calendar event for booking: {}", booking.getBookingId());

        // Use booking duration in minutes
        Duration duration = Duration.ofMinutes(booking.getDurationMinutes());

        Event event = new Event()
                .setSummary(booking.getServiceName())
                .setDescription("Booking ID: " + booking.getBookingId() + "\n" +
                               "Customer: " + booking.getCustomerId() + "\n" +
                               "Service: " + booking.getServiceDescription() + "\n" +
                               "Location: " + booking.getLocationAddress())
                .setStart(new EventDateTime()
                        .setDateTime(
                                new DateTime(Date.from(
                                        booking.getAppointmentDate()
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()
                                ))
                        )
                        .setTimeZone(ZoneId.systemDefault().toString())
                )
                .setEnd(new EventDateTime()
                        .setDateTime(new DateTime(Date.from(
                                booking.getAppointmentDate()
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .plus(duration)
                        )))
                        .setTimeZone(ZoneId.systemDefault().toString())
                );

        try {
            Event created = calendar.events()
                    .insert(calendarId, event)
                    .execute();

            log.info("Created calendar event: {} for booking: {}", created.getId(), booking.getBookingId());
            return created.getId();
        } catch (Exception ex) {
            log.error("Failed to create calendar event for booking: {}", booking.getBookingId(), ex);
            return null;
        }
    }

    @Override
    public void deleteEvent(Booking booking) {
        if (booking.getCalendarEventId() != null) {
            try {
                log.info("Deleting calendar event: {} for booking: {}", booking.getCalendarEventId(), booking.getBookingId());
                calendar.events()
                        .delete(calendarId, booking.getCalendarEventId())
                        .execute();
                log.info("Successfully deleted calendar event: {}", booking.getCalendarEventId());
            } catch (Exception e) {
                log.error("Failed to delete calendar event: {}", booking.getCalendarEventId(), e);
            }
        }
    }

    @Override
    public String updateEvent(Booking booking) {
        if (booking.getCalendarEventId() != null) {
            try {
                log.info("Updating calendar event: {} for booking: {}", booking.getCalendarEventId(), booking.getBookingId());

                // First delete the existing event
                deleteEvent(booking);

                // Then create a new one
                return createEvent(booking);
            } catch (Exception e) {
                log.error("Failed to update calendar event: {}", booking.getCalendarEventId(), e);
                return null;
            }
        } else {
            // Create new event if none exists
            return createEvent(booking);
        }
    }
}
