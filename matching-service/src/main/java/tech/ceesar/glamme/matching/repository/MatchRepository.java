package tech.ceesar.glamme.matching.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.matching.entity.Match;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    
    List<Match> findByCustomerId(String customerId);
    
    List<Match> findByStylistId(String stylistId);
    
    List<Match> findByCustomerIdAndStatus(String customerId, Match.Status status);
    
    List<Match> findByStylistIdAndStatus(String stylistId, Match.Status status);
    
    Optional<Match> findByCustomerIdAndStylistIdAndStatus(String customerId, String stylistId, Match.Status status);
    
    @Query("SELECT m FROM Match m WHERE m.status = :status AND m.expiresAt < :now")
    List<Match> findExpiredMatches(@Param("status") Match.Status status, @Param("now") LocalDateTime now);
    
    @Query("SELECT m FROM Match m WHERE m.customerId = :customerId ORDER BY m.matchScore DESC")
    List<Match> findByCustomerIdOrderByMatchScoreDesc(@Param("customerId") String customerId);
    
    @Query("SELECT m FROM Match m WHERE m.stylistId = :stylistId ORDER BY m.createdAt DESC")
    List<Match> findByStylistIdOrderByCreatedAtDesc(@Param("stylistId") String stylistId);
    
    @Query("SELECT COUNT(m) FROM Match m WHERE m.customerId = :customerId AND m.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") String customerId, @Param("status") Match.Status status);
    
    @Query("SELECT COUNT(m) FROM Match m WHERE m.stylistId = :stylistId AND m.status = :status")
    long countByStylistIdAndStatus(@Param("stylistId") String stylistId, @Param("status") Match.Status status);
}