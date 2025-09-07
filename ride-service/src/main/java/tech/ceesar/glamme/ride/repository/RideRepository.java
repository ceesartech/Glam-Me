package tech.ceesar.glamme.ride.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.ride.entity.Ride;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {
    
    Optional<Ride> findByRideId(String rideId);
    
    List<Ride> findByCustomerId(String customerId);
    
    List<Ride> findByDriverId(String driverId);
    
    List<Ride> findByCustomerIdAndStatus(String customerId, Ride.Status status);
    
    List<Ride> findByDriverIdAndStatus(String driverId, Ride.Status status);
    
    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.requestedAt < :cutoffDate")
    List<Ride> findExpiredRides(@Param("status") Ride.Status status, 
                               @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.requestedAt BETWEEN :startDate AND :endDate")
    List<Ride> findRidesByStatusAndDateRange(@Param("status") Ride.Status status,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(r) FROM Ride r WHERE r.driverId = :driverId AND r.status = :status")
    long countByDriverIdAndStatus(@Param("driverId") String driverId, @Param("status") Ride.Status status);
    
    @Query("SELECT COUNT(r) FROM Ride r WHERE r.customerId = :customerId AND r.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") String customerId, @Param("status") Ride.Status status);
    
    @Query("SELECT r FROM Ride r WHERE r.externalRideId = :externalRideId")
    Optional<Ride> findByExternalRideId(@Param("externalRideId") String externalRideId);
}