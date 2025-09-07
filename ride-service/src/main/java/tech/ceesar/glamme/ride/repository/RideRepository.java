package tech.ceesar.glamme.ride.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.ride.entity.RideRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<RideRequest, Long> {
    
    Optional<RideRequest> findByRideId(String rideId);

    Page<RideRequest> findByCustomerId(String customerId, Pageable pageable);

    Page<RideRequest> findByDriverId(String driverId, Pageable pageable);

    List<RideRequest> findByCustomerId(String customerId);

    List<RideRequest> findByDriverId(String driverId);

    List<RideRequest> findByCustomerIdAndStatus(String customerId, String status);

    List<RideRequest> findByDriverIdAndStatus(String driverId, String status);

    List<RideRequest> findByCustomerIdAndRequestedAtBetween(String customerId, LocalDateTime startDate, LocalDateTime endDate);

    List<RideRequest> findByDriverIdAndRequestedAtBetween(String driverId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT r FROM RideRequest r WHERE r.status = :status AND r.requestedAt < :cutoffDate")
    List<RideRequest> findExpiredRides(@Param("status") String status,
                                       @Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT r FROM RideRequest r WHERE r.status = :status AND r.requestedAt BETWEEN :startDate AND :endDate")
    List<RideRequest> findRidesByStatusAndDateRange(@Param("status") String status,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM RideRequest r WHERE r.driverId = :driverId AND r.status = :status")
    long countByDriverIdAndStatus(@Param("driverId") String driverId, @Param("status") String status);

    @Query("SELECT COUNT(r) FROM RideRequest r WHERE r.customerId = :customerId AND r.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") String customerId, @Param("status") String status);

    @Query("SELECT r FROM RideRequest r WHERE r.externalRideId = :externalRideId")
    Optional<RideRequest> findByExternalRideId(@Param("externalRideId") String externalRideId);
}