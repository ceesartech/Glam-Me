package tech.ceesar.glamme.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.booking.entity.StylistAvailability;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface StylistAvailabilityRepository extends JpaRepository<StylistAvailability, Long> {
    
    List<StylistAvailability> findByStylistId(String stylistId);
    
    List<StylistAvailability> findByStylistIdAndIsAvailableTrue(String stylistId);
    
    @Query("SELECT sa FROM StylistAvailability sa WHERE sa.stylistId = :stylistId AND sa.dayOfWeek = :dayOfWeek")
    List<StylistAvailability> findByStylistIdAndDayOfWeek(@Param("stylistId") String stylistId, 
                                                          @Param("dayOfWeek") StylistAvailability.DayOfWeek dayOfWeek);
    
    @Query("SELECT sa FROM StylistAvailability sa WHERE sa.stylistId = :stylistId AND sa.dayOfWeek = :dayOfWeek AND sa.isAvailable = true")
    List<StylistAvailability> findAvailableByStylistIdAndDayOfWeek(@Param("stylistId") String stylistId, 
                                                                   @Param("dayOfWeek") StylistAvailability.DayOfWeek dayOfWeek);
    
    @Query("SELECT sa FROM StylistAvailability sa WHERE sa.stylistId = :stylistId AND sa.dayOfWeek = :dayOfWeek AND sa.startTime <= :time AND sa.endTime >= :time")
    List<StylistAvailability> findByStylistIdAndDayOfWeekAndTimeRange(@Param("stylistId") String stylistId,
                                                                     @Param("dayOfWeek") StylistAvailability.DayOfWeek dayOfWeek,
                                                                     @Param("time") LocalTime time);
    
    void deleteByStylistId(String stylistId);
}