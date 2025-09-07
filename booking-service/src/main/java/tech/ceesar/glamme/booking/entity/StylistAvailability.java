package tech.ceesar.glamme.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "stylist_availability")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylistAvailability {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "stylist_id", nullable = false)
    private String stylistId;
    
    @Column(name = "day_of_week", nullable = false)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;
    
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;
    
    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "availability_services", joinColumns = @JoinColumn(name = "availability_id"))
    @Column(name = "service")
    private Set<String> availableServices;
    
    @Column(name = "max_bookings_per_slot")
    private Integer maxBookingsPerSlot;
    
    @Column(name = "break_duration_minutes")
    private Integer breakDurationMinutes;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    public enum Service {
        BASIC_CUT("Basic Haircut"),
        STYLING("Hair Styling"),
        COLORING("Hair Coloring"),
        TREATMENT("Hair Treatment"),
        MAKEUP("Makeup Application"),
        NAILS("Nail Service"),
        MASSAGE("Scalp Massage"),
        CONSULTATION("Style Consultation");

        private final String displayName;

        Service(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }
}