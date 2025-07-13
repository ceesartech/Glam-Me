package tech.ceesar.glamme.matching.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import tech.ceesar.glamme.matching.client.AuthClient;
import tech.ceesar.glamme.matching.dto.AddOnDto;
import tech.ceesar.glamme.matching.dto.OnboardingStylistResuest;
import tech.ceesar.glamme.matching.dto.ServiceOfferingDto;
import tech.ceesar.glamme.matching.entity.AddOn;
import tech.ceesar.glamme.matching.entity.ServiceOffering;
import tech.ceesar.glamme.matching.entity.StylistProfile;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StylistOnboardingService {
    private final StylistRepository stylistRepository;
    private final ServiceOfferingRepository offeringRepository;
    private final AuthClient authClient;

    @Transactional
    public void onboardStylist(OnboardingStylistResuest req) {
        // 1) Create and save the stylist profile
        StylistProfile profile = StylistProfile.builder()
                .userId(req.getUserId())
                .specialties(
                        req.getOfferings().stream()
                                .map(o -> o.getStyleName())
                                .collect(Collectors.toSet())
                )
                .eloRating(1500)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .build();
        profile = stylistRepository.save(profile);

        // 2) Persist each offering
        for (var dto : req.getOfferings()) {
            // build the offering
            ServiceOffering off = ServiceOffering.builder()
                    .stylistProfile(profile)
                    .styleName(dto.getStyleName())
                    .costPerHour(dto.getCostPerHour())
                    .estimatedHours(dto.getEstimatedHours())
                    .build();

            // <-- ensure addOns list is initialized -->
            off.setAddOns(new ArrayList<>());

            // add each add-on
            for (AddOnDto a : dto.getAddOns()) {
                off.getAddOns().add(
                        AddOn.builder()
                                .name(a.getName())
                                .cost(a.getCost())
                                .offering(off)
                                .build()
                );
            }

            offeringRepository.save(off);
        }

        // 3) Grant STYLIST role
        authClient.grantRole(req.getUserId(), "STYLIST")
                .block();
    }
}
