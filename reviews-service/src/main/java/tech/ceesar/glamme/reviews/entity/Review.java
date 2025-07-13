package tech.ceesar.glamme.reviews.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    @GeneratedValue
    private UUID reviewId;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "stylist_id", nullable = false)
    private UUID stylistId;

    @Column(nullable = false)
    private int rating;     // From 1 to 5

    @Column(length = 2000)
    private String comment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
