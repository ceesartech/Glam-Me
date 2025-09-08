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
import java.util.Set;

@Entity
@Table(name = "bookings")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_id", nullable = false, unique = true)
    private String bookingId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "stylist_id", nullable = false)
    private String stylistId;
    
    @Column(name = "service_id")
    private String serviceId;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "service_description", columnDefinition = "TEXT")
    private String serviceDescription;
    
    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;
    
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
    
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    @Column(name = "payment_intent_id")
    private String paymentIntentId;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_addons", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "addon")
    private Set<String> addons;
    
    @Column(name = "location_type")
    @Enumerated(EnumType.STRING)
    private LocationType locationType;
    
    @Column(name = "location_address")
    private String locationAddress;
    
    @Column(name = "location_latitude", precision = 10, scale = 8)
    private BigDecimal locationLatitude;
    
    @Column(name = "location_longitude", precision = 11, scale = 8)
    private BigDecimal locationLongitude;
    
    @Column(name = "calendar_event_id")
    private String calendarEventId;
    
    @Column(name = "google_calendar_id")
    private String googleCalendarId;
    
    @Column(name = "apple_calendar_url")
    private String appleCalendarUrl;
    
    @Column(name = "confirmation_code")
    private String confirmationCode;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "reminder_sent")
    private Boolean reminderSent;
    
    @Column(name = "confirmation_sent")
    private Boolean confirmationSent;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Status {
        PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW, RESCHEDULED
    }
    
    public enum PaymentStatus {
        PENDING, HELD, CAPTURED, REFUNDED, FAILED
    }
    
    public enum LocationType {
        SALON, HOME, MOBILE, VIRTUAL
    }

    public enum ServiceType {
        HAIRCUT, STYLING, COLORING, TREATMENT, MAKEUP, NAILS, MASSAGE, CONSULTATION
    }

    public enum Service {
        BASIC_CUT("Basic Haircut", 30, ServiceType.HAIRCUT),
        STYLING("Hair Styling", 45, ServiceType.STYLING),
        COLORING("Hair Coloring", 120, ServiceType.COLORING),
        TREATMENT("Hair Treatment", 60, ServiceType.TREATMENT),
        MAKEUP("Makeup Application", 90, ServiceType.MAKEUP),
        NAILS("Nail Service", 60, ServiceType.NAILS),
        MASSAGE("Scalp Massage", 30, ServiceType.MASSAGE),
        CONSULTATION("Style Consultation", 30, ServiceType.CONSULTATION);

        private final String displayName;
        private final int defaultDurationMinutes;
        private final ServiceType type;

        Service(String displayName, int defaultDurationMinutes, ServiceType type) {
            this.displayName = displayName;
            this.defaultDurationMinutes = defaultDurationMinutes;
            this.type = type;
        }

        public String getDisplayName() { return displayName; }
        public int getDefaultDurationMinutes() { return defaultDurationMinutes; }
        public ServiceType getType() { return type; }
    }
}