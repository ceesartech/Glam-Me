package tech.ceesar.glamme.booking.service;

import tech.ceesar.glamme.booking.entity.Booking;

/**
 * Calendar service interface for calendar integration
 */
public interface CalendarService {

    /**
     * Create a calendar event for the booking
     * @param booking The booking to create calendar event for
     * @return The calendar event ID
     */
    String createEvent(Booking booking);

    /**
     * Delete a calendar event for the booking
     * @param booking The booking whose calendar event should be deleted
     */
    void deleteEvent(Booking booking);

    /**
     * Update a calendar event for the booking
     * @param booking The booking to update calendar event for
     * @return The calendar event ID
     */
    String updateEvent(Booking booking);
}