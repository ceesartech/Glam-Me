package tech.ceesar.glamme.social.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "post_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostTag {
    @Id
    @GeneratedValue
    private UUID tagId;

    @Column(nullable = false)
    private UUID stylistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
}
