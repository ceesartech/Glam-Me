package tech.ceesar.glamme.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.booking.entity.BookingTimeSlot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for BookingTimeSlot entity
 */
@Repository
public interface BookingTimeSlotRepository extends JpaRepository<BookingTimeSlot, Long> {

    /**
     * Find available time slots by stylist ID and date range
     */
    @Query("SELECT b FROM BookingTimeSlot b WHERE b.stylistId = :stylistId AND b.slotDate BETWEEN :startDate AND :endDate AND b.isAvailable = true")
    List<BookingTimeSlot> findAvailableByStylistIdAndSlotDateBetween(
            @Param("stylistId") String stylistId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find available time slots by stylist ID and start time
     */
    @Query("SELECT b FROM BookingTimeSlot b WHERE b.stylistId = :stylistId AND b.startTime = :startTime AND b.isAvailable = true")
    List<BookingTimeSlot> findAvailableByStylistIdAndStartTime(
            @Param("stylistId") String stylistId,
            @Param("startTime") LocalDateTime startTime);

    /**
     * Find time slots by stylist ID
     */
    List<BookingTimeSlot> findByStylistId(String stylistId);

    /**
     * Find time slots by booking ID
     */
    List<BookingTimeSlot> findByBookingId(Long bookingId);
}
