package tech.ceesar.glamme.ride.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.ride.dto.DriverLocationUpdateRequest;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {
    private final DriverProfileRepository driverRepo;
    private final SimpMessagingTemplate bus;

    @PostMapping("/{driverId}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable UUID driverId,
            @RequestBody DriverLocationUpdateRequest req
    ) {
        var driver = driverRepo.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId.toString()));
        driver.setCurrentLatitude(req.getLatitude());
        driver.setCurrentLongitude(req.getLongitude());
        driverRepo.save(driver);
        // broadcast to everyone
        bus.convertAndSend("/topic/driver-locations", req);
        return ResponseEntity.ok().build();
    }
}
