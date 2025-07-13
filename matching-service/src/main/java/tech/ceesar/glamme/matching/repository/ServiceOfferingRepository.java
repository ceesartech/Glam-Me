package tech.ceesar.glamme.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.matching.entity.ServiceOffering;

import java.util.List;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    /**
     * Find all offerings that match exactly this style name.
     */
    List<ServiceOffering> findByStyleName(String styleName);
}
