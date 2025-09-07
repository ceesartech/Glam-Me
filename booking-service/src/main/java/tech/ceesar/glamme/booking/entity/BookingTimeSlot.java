package tech.ceesar.glamme.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing time slots for bookings
 */
@Entity
@Table(name = "booking_time_slots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stylist_id", nullable = false)
    private String stylistId;

    @Column(name = "slot_date", nullable = false)
    private LocalDateTime slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
