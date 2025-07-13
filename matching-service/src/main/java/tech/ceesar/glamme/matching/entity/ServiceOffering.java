package tech.ceesar.glamme.matching.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_offerings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceOffering {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stylist_profile_id", nullable = false)
    private StylistProfile stylistProfile;

    @Column(nullable = false)
    private String styleName;

    @Column(nullable = false)
    private double costPerHour;

    @Column(nullable = false)
    private double estimatedHours;

    @OneToMany(
            mappedBy = "offering",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AddOn> addOns = new ArrayList<>();
}
