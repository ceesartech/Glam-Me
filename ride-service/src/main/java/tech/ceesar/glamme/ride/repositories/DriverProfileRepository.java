package tech.ceesar.glamme.ride.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.ride.entity.DriverProfile;

import java.util.List;
import java.util.UUID;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {
    List<DriverProfile> findByAvailableTrue();
}
