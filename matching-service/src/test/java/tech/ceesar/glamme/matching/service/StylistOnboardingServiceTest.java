package tech.ceesar.glamme.matching.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tech.ceesar.glamme.matching.client.AuthClient;
import tech.ceesar.glamme.matching.dto.AddOnDto;
import tech.ceesar.glamme.matching.dto.OnboardingStylistResuest;
import tech.ceesar.glamme.matching.dto.ServiceOfferingDto;
import tech.ceesar.glamme.matching.entity.StylistProfile;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;
import tech.ceesar.glamme.matching.repository.StylistRepository;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StylistOnboardingServiceTest {

    @Mock
    StylistRepository profileRepo;
    @Mock
    ServiceOfferingRepository offeringRepo;
    @Mock
    AuthClient authClient;               // <-- mock only this!

    @InjectMocks
    StylistOnboardingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Grant succeeds:
        when(authClient.grantRole(any(), any()))
                .thenReturn(reactor.core.publisher.Mono.empty());
    }

    @Test
    void onboardStylist_savesProfileOfferingsAndGrantsRole() {
        // Arrange
        UUID userId = UUID.randomUUID();
        OnboardingStylistResuest req = OnboardingStylistResuest.builder()
                .userId(userId)
                .latitude(40.0)
                .longitude(-105.0)
                .offerings(List.of(
                        ServiceOfferingDto.builder()
                                .styleName("bob")
                                .costPerHour(50)
                                .estimatedHours(2.0)
                                .addOns(List.of(new AddOnDto("deep-condition", 20)))
                                .build()
                ))
                .build();

        StylistProfile savedProfile = StylistProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .build();
        when(profileRepo.save(any(StylistProfile.class)))
                .thenReturn(savedProfile);
        when(offeringRepo.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        service.onboardStylist(req);

        // Assert
        verify(profileRepo).save(any(StylistProfile.class));
        verify(offeringRepo).save(any());
        verify(authClient).grantRole(userId, "STYLIST");
    }
}
