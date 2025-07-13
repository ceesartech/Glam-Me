package tech.ceesar.glamme.matching.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.enums.SubscriptionType;
import tech.ceesar.glamme.matching.dto.*;
import tech.ceesar.glamme.matching.entity.ServiceOffering;
//import tech.ceesar.glamme.matching.entity.StylistProfile;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final ServiceOfferingRepository offeringRepo;
    private final StylistRepository profileRepo;

    public List<OfferingResponse> recommendOfferings(
            String styleName,
            double userLat,
            double userLon,
            int limit
    ) {
        List<ServiceOffering> offerings = offeringRepo.findByStyleName(styleName);
        if (offerings.isEmpty()) {
            offerings = offeringRepo.findAll();
        }

        return offerings.stream()
                .map(off -> {
                    var p = off.getStylistProfile();
                    double distance = computeDistance(
                            userLat, userLon, p.getLatitude(), p.getLongitude()
                    );
                    // ----- Null‑safe addOns mapping -----
                    List<AddOnDto> addons = Optional
                            .ofNullable(off.getAddOns())
                            .orElse(Collections.emptyList())
                            .stream()
                            .map(a -> AddOnDto.builder()
                                    .name(a.getName())
                                    .cost(a.getCost())
                                    .build())
                            .collect(Collectors.toList());
                    // ------------------------------------

                    return OfferingResponse.builder()
                            .offeringId(off.getId())
                            .stylistProfileId(p.getId())
                            .userId(p.getUserId())
                            .specialties(p.getSpecialties())
                            .eloRating(p.getEloRating())
                            .distance(distance)
                            .styleName(off.getStyleName())
                            .costPerHour(off.getCostPerHour())
                            .estimatedHours(off.getEstimatedHours())
                            .addOns(addons)
                            .build();
                })
                .sorted(Comparator
                        .comparingDouble(OfferingResponse::getEloRating).reversed()
                        .thenComparingDouble(OfferingResponse::getTotalCost)
                        .thenComparingDouble(OfferingResponse::getDistance)
                )
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Haversine formula to compute distance (in km) between two geo points.
     */
    private double computeDistance(
            double lat1, double lon1, double lat2, double lon2
    ) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Update the Elo rating.
     */
    public double updateElo(double ratingA, double ratingB, boolean aWon) {
        double k = 32;
        double expectedA = 1.0 / (1 + Math.pow(10, ((ratingB - ratingA) / 400)));
        double scoreA = aWon ? 1.0 : 0.0;
        return ratingA + k * (scoreA - expectedA);
    }

    /**
     * Gale–Shapley stable matching between customers and stylists.
     * Stylists prefer PREMIUM over FREE customers.
     * Customers prefer highest-Elo & lowest-cost stylists.
     */
    public List<PairDto> stableMatch(StableMatchRequest req) {
        List<CustomerDto> customers = req.getCustomers();
        List<StylistDto> stylists = req.getStylists();

        // Map for stylist preferences of customers
        Map<UUID, SubscriptionType> custSub = customers.stream()
                .collect(Collectors.toMap(
                        CustomerDto::getId,
                        CustomerDto::getSubscriptionType
                ));

        // Build customer → queue of stylist IDs in preference order
        Map<UUID, Deque<UUID>> custPrefs = customers.stream().collect(
                Collectors.toMap(
                        CustomerDto::getId,
                        c -> stylists.stream()
                                .sorted(Comparator
                                        .comparingDouble(StylistDto::getEloRating).reversed()
                                        .thenComparingDouble(StylistDto::getCostPerHour)
                                )
                                .map(StylistDto::getId)
                                .collect(Collectors.toCollection(ArrayDeque::new))
                )
        );

        Map<UUID, UUID> matches = new HashMap<>(); // stylistId → customerId
        Set<UUID> freeCustomers = new HashSet<>(custPrefs.keySet());

        while (!freeCustomers.isEmpty()) {
            UUID custId = freeCustomers.iterator().next();
            Deque<UUID> queue = custPrefs.get(custId);
            if (queue == null || queue.isEmpty()) {
                freeCustomers.remove(custId);
                continue;
            }
            UUID stylistId = queue.poll();
            if (!matches.containsKey(stylistId)) {
                matches.put(stylistId, custId);
                freeCustomers.remove(custId);
            } else {
                UUID currentCust = matches.get(stylistId);
                // Does stylist prefer this cust over current?
                if (prefers(custSub, custId, currentCust)) {
                    matches.put(stylistId, custId);
                    freeCustomers.remove(custId);
                    freeCustomers.add(currentCust);
                }
                // else keep current, continue with same free cust
            }
        }

        return matches.entrySet().stream()
                .map(e -> new PairDto(e.getValue(), e.getKey()))
                .collect(Collectors.toList());
    }

    private boolean prefers(Map<UUID, SubscriptionType> customerSubType,
                            UUID newCustomer,
                            UUID currentCustomer
    ) {
        SubscriptionType n = customerSubType.get(newCustomer);
        SubscriptionType c = customerSubType.get(currentCustomer);
        if (n == SubscriptionType.FREE && c == SubscriptionType.FREE) {
            return true;
        }
        return false;
    }
}
