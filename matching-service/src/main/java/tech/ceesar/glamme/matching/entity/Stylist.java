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
@Table(name = "stylists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stylist {
    
    @Id
    @Column(name = "id")
    private String id; // User ID from auth service
    
    @Column(name = "business_name", nullable = false)
    private String businessName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "zip_code")
    private String zipCode;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "instagram_handle")
    private String instagramHandle;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Column(name = "portfolio_images", columnDefinition = "TEXT[]")
    private String[] portfolioImages;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stylist_specialties", joinColumns = @JoinColumn(name = "stylist_id"))
    @Column(name = "specialty")
    @Enumerated(EnumType.STRING)
    private Set<Specialty> specialties;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stylist_services", joinColumns = @JoinColumn(name = "stylist_id"))
    @Column(name = "service")
    @Enumerated(EnumType.STRING)
    private Set<Service> services;
    
    @Column(name = "price_range_min", precision = 10, scale = 2)
    private BigDecimal priceRangeMin;
    
    @Column(name = "price_range_max", precision = 10, scale = 2)
    private BigDecimal priceRangeMax;
    
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;
    
    @Column(name = "total_reviews")
    private Integer totalReviews;
    
    @Column(name = "elo_rating")
    private Integer eloRating;
    
    @Column(name = "is_verified")
    private Boolean isVerified;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "certifications", columnDefinition = "TEXT[]")
    private String[] certifications;

    @Column(name = "languages", columnDefinition = "TEXT[]")
    private String[] languages;

    // Additional fields for compatibility
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "service_radius")
    private Integer serviceRadius;

    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "certification")
    private String certification;

    @Column(name = "portfolio_url")
    private String portfolioUrl;

    @Column(name = "is_available")
    private Boolean isAvailable;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    public enum Specialty {
        HAIR_CUTTING, HAIR_COLORING, HAIR_STYLING, HAIR_EXTENSIONS,
        MAKEUP, EYEBROW_SHAPING, LASH_EXTENSIONS, FACIAL_TREATMENTS,
        NAIL_ART, MANICURE, PEDICURE, SKIN_CARE, BRIDAL, SPECIAL_EVENTS
    }
    
    public enum Service {
        CONSULTATION, HAIR_CUT, HAIR_COLOR, HIGHLIGHTS, BALAYAGE,
        BLOWOUT, UPDO, BRAIDS, MAKEUP_APPLICATION, EYEBROW_SHAPING,
        LASH_EXTENSIONS, FACIAL, MANICURE, PEDICURE, NAIL_ART,
        BRIDAL_PACKAGE, SPECIAL_EVENT_PACKAGE
    }
}