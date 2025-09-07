package tech.ceesar.glamme.ride.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.ride.entity.RideTracking;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideTrackingRepository extends JpaRepository<RideTracking, Long> {
    
    List<RideTracking> findByRideId(String rideId);
    
    List<RideTracking> findByDriverId(String driverId);
    
    @Query("SELECT rt FROM RideTracking rt WHERE rt.rideId = :rideId ORDER BY rt.timestamp DESC")
    List<RideTracking> findByRideIdOrderByTimestampDesc(@Param("rideId") String rideId);
    
    @Query("SELECT rt FROM RideTracking rt WHERE rt.driverId = :driverId AND rt.timestamp BETWEEN :startTime AND :endTime")
    List<RideTracking> findByDriverIdAndTimestampBetween(@Param("driverId") String driverId,
                                                        @Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT rt FROM RideTracking rt WHERE rt.rideId = :rideId AND rt.timestamp = (SELECT MAX(rt2.timestamp) FROM RideTracking rt2 WHERE rt2.rideId = :rideId)")
    RideTracking findLatestByRideId(@Param("rideId") String rideId);
}