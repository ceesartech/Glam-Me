package tech.ceesar.glamme.matching.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "customer_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "max_distance_km")
    private Integer maxDistanceKm;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_preferred_specialties", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "specialty")
    @Enumerated(EnumType.STRING)
    private Set<Stylist.Specialty> preferredSpecialties;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_preferred_services", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "service")
    @Enumerated(EnumType.STRING)
    private Set<Stylist.Service> preferredServices;
    
    @Column(name = "price_range_min", precision = 10, scale = 2)
    private BigDecimal priceRangeMin;
    
    @Column(name = "price_range_max", precision = 10, scale = 2)
    private BigDecimal priceRangeMax;
    
    @Column(name = "min_rating", precision = 3, scale = 2)
    private BigDecimal minRating;
    
    @Column(name = "prefer_verified")
    private Boolean preferVerified;
    
    @Column(name = "prefer_experienced")
    private Boolean preferExperienced;
    
    @Column(name = "min_years_experience")
    private Integer minYearsExperience;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_preferred_languages", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "language")
    private Set<String> preferredLanguages;
    
    @Column(name = "availability_preferences", columnDefinition = "TEXT")
    private String availabilityPreferences; // JSON string
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Style {
        MODERN,
        CLASSIC,
        CONTEMPORARY,
        TRADITIONAL,
        ECLECTIC,
        MINIMALIST,
        BOHEMIAN,
        INDUSTRIAL,
        SCANDINAVIAN,
        MEDITERRANEAN
    }

    public enum ExperienceLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }
}