package tech.ceesar.glamme.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.booking.entity.BookingReminder;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingReminderRepository extends JpaRepository<BookingReminder, Long> {
    
    List<BookingReminder> findByBookingId(String bookingId);
    
    List<BookingReminder> findByStatus(BookingReminder.Status status);
    
    @Query("SELECT br FROM BookingReminder br WHERE br.status = :status AND br.scheduledTime <= :currentTime")
    List<BookingReminder> findPendingReminders(@Param("status") BookingReminder.Status status, 
                                             @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT br FROM BookingReminder br WHERE br.bookingId = :bookingId AND br.reminderType = :reminderType")
    List<BookingReminder> findByBookingIdAndReminderType(@Param("bookingId") String bookingId, 
                                                         @Param("reminderType") BookingReminder.ReminderType reminderType);
    
    @Query("SELECT COUNT(br) FROM BookingReminder br WHERE br.status = :status")
    long countByStatus(@Param("status") BookingReminder.Status status);
    
    @Query("SELECT br FROM BookingReminder br WHERE br.status = :status AND br.retryCount < :maxRetries AND br.scheduledTime <= :currentTime")
    List<BookingReminder> findFailedRemindersForRetry(@Param("status") BookingReminder.Status status,
                                                     @Param("maxRetries") Integer maxRetries,
                                                     @Param("currentTime") LocalDateTime currentTime);
}
