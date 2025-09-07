package tech.ceesar.glamme.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.matching.entity.CustomerPreference;

import java.util.Optional;

@Repository
public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, Long> {
    
    Optional<CustomerPreference> findByCustomerId(String customerId);
    
    @Query("SELECT cp FROM CustomerPreference cp WHERE cp.customerId = :customerId")
    Optional<CustomerPreference> findActivePreferenceByCustomerId(@Param("customerId") String customerId);
    
    void deleteByCustomerId(String customerId);
}