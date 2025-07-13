package tech.ceesar.glamme.social.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follows", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followed_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {
    @Id
    @GeneratedValue
    private UUID followId;

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "followed_id", nullable = false)
    private UUID followedId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
