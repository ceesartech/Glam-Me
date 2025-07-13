package tech.ceesar.glamme.matching.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stylist_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StylistProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stylist_specialties", joinColumns = @JoinColumn(name = "stylist_id"))
    @Column(name = "specialty")
    private Set<String> specialties;

    @Column(nullable = false)
    private double eloRating;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;
}
