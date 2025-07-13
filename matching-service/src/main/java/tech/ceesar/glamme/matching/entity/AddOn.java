package tech.ceesar.glamme.matching.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "add_ons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddOn {
    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private ServiceOffering offering;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double cost;
}
