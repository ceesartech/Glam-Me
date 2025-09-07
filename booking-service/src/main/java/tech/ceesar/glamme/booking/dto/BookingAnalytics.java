package tech.ceesar.glamme.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Booking analytics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingAnalytics {
    private int totalBookings;
    private int confirmedBookings;
    private int completedBookings;
    private int cancelledBookings;
    private int noShowBookings;
    private BigDecimal totalRevenue;
    private BigDecimal averageBookingValue;
    private double completionRate;
    private double cancellationRate;
    private double noShowRate;
    private int upcomingBookings;
    private int todaysBookings;
    private int weeklyBookings;
    private int monthlyBookings;
    private double averageRating;
    private int totalReviews;
}
