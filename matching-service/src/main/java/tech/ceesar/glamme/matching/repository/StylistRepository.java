package tech.ceesar.glamme.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.ceesar.glamme.matching.entity.StylistProfile;

import java.util.List;
import java.util.UUID;

public interface StylistRepository extends JpaRepository<StylistProfile, UUID> {
    @Query("SELECT s FROM StylistProfile s WHERE :hair MEMBER OF s.specialties")
    List<StylistProfile> findStylistBySpecialty(@Param("hair") String hairStyleType);
}
