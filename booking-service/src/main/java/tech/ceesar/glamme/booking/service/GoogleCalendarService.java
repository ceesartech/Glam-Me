package tech.ceesar.glamme.booking.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.booking.entity.Booking;
import tech.ceesar.glamme.matching.entity.ServiceOffering;

import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService implements CalendarService {
    private final Calendar calendar;
    public ServiceOffering serviceOffering;

    @Value("${google.calendar.id}")
    private String calendarId;

    @Override
    public String createEvent(Booking booking) {
        double appointmentDurationInSeconds = serviceOffering.getEstimatedHours() * 3600;
        TemporalAmount calendarDuration = convertDoubleToTemporalAmount(appointmentDurationInSeconds);
        Event event = new Event()
                .setSummary("Hair Appointment")
                .setDescription("Booking ID: " + booking.getBookingId())
                .setStart(new EventDateTime()
                        .setDateTime(
                                new DateTime(Date.from(
                                        booking.getScheduledTime()
                                                .atZone(ZoneId.systemDefault())
                                                .toInstant()
                                ))
                        )
                        .setTimeZone(ZoneId.systemDefault().toString())
                )
                .setEnd(new EventDateTime()
                        .setDateTime(new DateTime(Date.from(
                                booking.getScheduledTime()
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .plus(calendarDuration)     // AppointmentDuration
                        )))
                        .setTimeZone(ZoneId.systemDefault().toString())
                );

        try {
            Event created = calendar.events()
                    .insert(calendarId, event)
                    .execute();
            return created.getId();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void deleteEvent(Booking booking) {
        if (booking.getCalendarEventId() != null) {
            try {
                calendar.events()
                        .delete(calendarId, booking.getCalendarEventId())
                        .execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private TemporalAmount convertDoubleToTemporalAmount(double seconds) {
        return Duration.ofMillis((long) (seconds * 1000));
    }
}
