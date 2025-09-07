package tech.ceesar.glamme.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.booking.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    Optional<Booking> findByBookingId(String bookingId);
    
    List<Booking> findByCustomerId(String customerId);
    
    List<Booking> findByStylistId(String stylistId);
    
    List<Booking> findByCustomerIdAndStatus(String customerId, Booking.Status status);
    
    List<Booking> findByStylistIdAndStatus(String stylistId, Booking.Status status);
    
    @Query("SELECT b FROM Booking b WHERE b.appointmentDate BETWEEN :startDate AND :endDate")
    List<Booking> findByAppointmentDateBetween(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.stylistId = :stylistId AND b.appointmentDate BETWEEN :startDate AND :endDate")
    List<Booking> findByStylistIdAndAppointmentDateBetween(@Param("stylistId") String stylistId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.appointmentDate < :cutoffDate")
    List<Booking> findExpiredBookings(@Param("status") Booking.Status status, 
                                    @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.appointmentDate BETWEEN :startDate AND :endDate")
    List<Booking> findUpcomingBookings(@Param("status") Booking.Status status,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.stylistId = :stylistId AND b.status = :status")
    long countByStylistIdAndStatus(@Param("stylistId") String stylistId, @Param("status") Booking.Status status);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.customerId = :customerId AND b.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") String customerId, @Param("status") Booking.Status status);
    
    @Query("SELECT b FROM Booking b WHERE b.confirmationCode = :confirmationCode")
    Optional<Booking> findByConfirmationCode(@Param("confirmationCode") String confirmationCode);
}