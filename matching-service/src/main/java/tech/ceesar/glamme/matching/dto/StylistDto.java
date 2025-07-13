package tech.ceesar.glamme.matching.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class StylistDto {
    private UUID id;
    private UUID userId;
    private Set<String> specialties;
    private double costPerHour;
    private double eloRating;
    private double distance;
}
