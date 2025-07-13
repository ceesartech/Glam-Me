package tech.ceesar.glamme.booking.service;

import tech.ceesar.glamme.booking.entity.Booking;

public interface CalendarService {
    /**
     * Create an event in the calendar for this booking.
     * @return the created event ID
     */
    String createEvent(Booking booking);

    /**
     * Delete the calendar event for this booking, if any.
     */
    void deleteEvent(Booking booking);
}
