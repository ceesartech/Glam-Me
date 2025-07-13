package tech.ceesar.glamme.ride.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.enums.RideStatus;

import java.util.List;
import java.util.UUID;

public interface RideRequestRepository extends JpaRepository<RideRequest, UUID> {
    List<RideRequest> findByCustomerId(UUID customerId);
    List<RideRequest> findByStatus(RideStatus status);
}
