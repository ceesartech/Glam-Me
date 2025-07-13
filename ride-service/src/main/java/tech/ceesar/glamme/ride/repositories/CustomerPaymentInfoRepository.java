package tech.ceesar.glamme.ride.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.ride.entity.CustomerPaymentInfo;

import java.util.Optional;
import java.util.UUID;

public interface CustomerPaymentInfoRepository extends JpaRepository<CustomerPaymentInfo, Long> {
    Optional<CustomerPaymentInfo> findByUserId(UUID userId);
}
