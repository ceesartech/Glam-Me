package tech.ceesar.glamme.matching.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.ceesar.glamme.matching.dto.OnboardingStylistResuest;
import tech.ceesar.glamme.matching.service.StylistOnboardingService;

@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class OnboardingController {
    private final StylistOnboardingService onboardingService;

    /**
     * Stylist on-boarding: submit location + offering details,
     * then auto-grant STYLIST role.
     */
    @PostMapping("/onboard-stylist")
    public ResponseEntity<Void> onboardStylist(
            @Valid @RequestBody OnboardingStylistResuest onboardingRequest) {
        onboardingService.onboardStylist(onboardingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
