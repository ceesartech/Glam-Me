package tech.ceesar.glamme.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tech.ceesar.glamme.booking.enums.BookingStatus;
import tech.ceesar.glamme.booking.enums.PaymentStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {
    @Id
    @GeneratedValue
    private UUID bookingId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private UUID stylistId;

    @Column(nullable = false)
    private UUID offeringId;

    @Column(nullable = true)
    private String calendarEventId;

    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
