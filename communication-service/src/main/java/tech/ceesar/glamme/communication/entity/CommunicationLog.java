package tech.ceesar.glamme.communication.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tech.ceesar.glamme.communication.enums.Channel;
import tech.ceesar.glamme.communication.enums.Direction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communication_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationLog {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = true)
    private UUID fromUserId;

    @Column(nullable = true)
    private UUID toUserId;

    @Column(nullable = false)
    private String fromNumber;

    @Column(nullable = false)
    private String toNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(length = 3000)
    private String messageBody;

    @Column(nullable = true)
    private String sid;     // messageSid or callSid

    @Column(nullable = true)
    private String status;      // e.g. "queued", "delivered", "completed"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
