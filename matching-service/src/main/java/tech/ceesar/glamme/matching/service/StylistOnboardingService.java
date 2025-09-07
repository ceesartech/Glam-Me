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
import tech.ceesar.glamme.matching.entity.Stylist;
import tech.ceesar.glamme.matching.entity.StylistProfile;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.math.BigDecimal;

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
        Stylist stylist = Stylist.builder()
                .id(req.getUserId().toString())
                .businessName(req.getBusinessName())
                .description(req.getDescription())
                .latitude(BigDecimal.valueOf(req.getLatitude()))
                .longitude(BigDecimal.valueOf(req.getLongitude()))
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .zipCode(req.getZipCode())
                .phoneNumber(req.getPhoneNumber())
                .email(req.getEmail())
                .website(req.getWebsite())
                .instagramHandle(req.getInstagramHandle())
                .profileImageUrl(req.getProfileImageUrl())
                .portfolioImages(req.getPortfolioImages())
                .specialties(req.getSpecialties())
                .services(req.getServices())
                .priceRangeMin(req.getPriceRangeMin())
                .priceRangeMax(req.getPriceRangeMax())
                .averageRating(BigDecimal.ZERO)
                .totalReviews(0)
                .eloRating(1200)
                .isVerified(false)
                .isActive(true)
                .yearsExperience(req.getYearsExperience())
                .certifications(req.getCertifications())
                .languages(req.getLanguages())
                .build();
        stylist = stylistRepository.save(stylist);

        // 2) Persist each offering
        for (var dto : req.getOfferings()) {
            // build the offering
            ServiceOffering off = ServiceOffering.builder()
                    .stylist(stylist)
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
