package tech.ceesar.glamme.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.matching.entity.Stylist;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StylistRepository extends JpaRepository<Stylist, String> {
    
    List<Stylist> findByIsActiveTrue();
    
    List<Stylist> findByIsActiveTrueAndIsVerifiedTrue();
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true AND " +
           "ST_DWithin(ST_Point(s.longitude, s.latitude), ST_Point(:longitude, :latitude), :distanceKm * 1000)")
    List<Stylist> findNearbyStylists(@Param("latitude") BigDecimal latitude, 
                                   @Param("longitude") BigDecimal longitude, 
                                   @Param("distanceKm") Double distanceKm);
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true AND " +
           "s.priceRangeMin <= :maxPrice AND s.priceRangeMax >= :minPrice")
    List<Stylist> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                 @Param("maxPrice") BigDecimal maxPrice);
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true AND " +
           "s.averageRating >= :minRating")
    List<Stylist> findByMinRating(@Param("minRating") BigDecimal minRating);
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true AND " +
           ":specialty MEMBER OF s.specialties")
    List<Stylist> findBySpecialty(@Param("specialty") Stylist.Specialty specialty);
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true AND " +
           ":service MEMBER OF s.services")
    List<Stylist> findByService(@Param("service") Stylist.Service service);
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true AND " +
           "s.yearsExperience >= :minYears")
    List<Stylist> findByMinExperience(@Param("minYears") Integer minYears);
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true ORDER BY s.eloRating DESC")
    List<Stylist> findAllOrderByEloRatingDesc();
    
    @Query("SELECT s FROM Stylist s WHERE s.isActive = true ORDER BY s.averageRating DESC")
    List<Stylist> findAllOrderByAverageRatingDesc();

    @Query("SELECT s FROM Stylist s WHERE s.isActive = true AND s.isVerified = true AND s.isAvailable = true")
    List<Stylist> findByIsAvailableTrueAndIsVerifiedTrue();
    
    @Query("SELECT COUNT(s) FROM Stylist s WHERE s.isActive = true")
    long countActiveStylists();
    
    @Query("SELECT COUNT(s) FROM Stylist s WHERE s.isActive = true AND s.isVerified = true")
    long countVerifiedStylists();
}