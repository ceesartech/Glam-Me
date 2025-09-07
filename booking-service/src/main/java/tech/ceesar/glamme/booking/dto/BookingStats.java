package tech.ceesar.glamme.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Booking statistics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStats {
    private long totalBookings;
    private long pendingBookings;
    private long confirmedBookings;
    private long completedBookings;
    private long cancelledBookings;
    private long noShowBookings;
    private long upcomingBookings;
    private long pastBookings;
    private double completionRate;
    private double cancellationRate;
    private double noShowRate;
}
