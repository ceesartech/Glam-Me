package tech.ceesar.glamme.ride.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.common.service.RedisCacheService;
import tech.ceesar.glamme.common.service.RedisIdempotencyService;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;
import tech.ceesar.glamme.ride.repositories.RideRequestRepository;
import tech.ceesar.glamme.ride.service.aws.RideEventService;
import tech.ceesar.glamme.ride.service.aws.RideProviderService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @Mock
    private RideRequestRepository rideRequestRepository;

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RideProviderService rideProviderService;

    @Mock
    private RideEventService rideEventService;

    @Mock
    private RedisCacheService cacheService;

    @Mock
    private RedisIdempotencyService idempotencyService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private RideService rideService;
    
    private CreateRideRequest createRideRequest;
    private RideRequest sampleRideRequest;
    private DriverProfile sampleDriver;
    private LocationDto pickupLocation;
    private LocationDto dropoffLocation;
    private UUID customerId;
    private UUID rideId;

    @BeforeEach
    void setUp() {
        // Manually inject PaymentService since it's @Autowired
        ReflectionTestUtils.setField(rideService, "paymentService", paymentService);
        
        customerId = UUID.randomUUID();
        rideId = UUID.randomUUID();

        pickupLocation = LocationDto.builder()
                .latitude(BigDecimal.valueOf(40.7128))
                .longitude(BigDecimal.valueOf(-74.0060))
                .build();

        dropoffLocation = LocationDto.builder()
                .latitude(BigDecimal.valueOf(40.7589))
                .longitude(BigDecimal.valueOf(-73.9851))
                .build();

        // Create request without builder (since it doesn't have @Builder)
        createRideRequest = new CreateRideRequest();
        createRideRequest.setCustomerId(customerId);
        createRideRequest.setProviderType(ProviderType.INTERNAL);
        createRideRequest.setPickupLocation(pickupLocation);
        createRideRequest.setDropoffLocation(dropoffLocation);
        createRideRequest.setProductId("standard");

        sampleRideRequest = RideRequest.builder()
                .rideRequestId(rideId)
                .customerId(customerId)
                .providerType(ProviderType.INTERNAL)
                .pickupLatitude(40.7128) // Use double directly
                .pickupLongitude(-74.0060)
                .dropoffLatitude(40.7589)
                .dropoffLongitude(-73.9851)
                .status(RideStatus.REQUESTED)
                .requestTime(Instant.now())
                .estimatedFare(25.00)
                .actualFare(0.0) // Initially 0
                .currency("USD")
                .build();

        sampleDriver = DriverProfile.builder()
                .driverId(UUID.randomUUID())
                .driverName("John Driver")
                .phoneNumber("+1234567890")
                .vehicleModel("Toyota Camry")
                .vehicleLicense("ABC123")
                .currentLatitude(BigDecimal.valueOf(40.7128))
                .currentLongitude(BigDecimal.valueOf(-74.0060))
                .available(true)
                .online(true)
                .rating(BigDecimal.valueOf(4.8))
                .ridesCompletedToday(150)
                .build();
    }

    @Test
    void createRide_IdempotencyCheck_Success() {
        // Arrange
        when(idempotencyService.startRideOperation(anyString(), anyString(), any())).thenReturn(true);
        when(driverProfileRepository.findByAvailableTrue()).thenReturn(List.of(sampleDriver));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(sampleDriver);
        when(rideRequestRepository.save(any(RideRequest.class))).thenAnswer(invocation -> {
            RideRequest request = invocation.getArgument(0);
            return RideRequest.builder()
                    .rideRequestId(rideId)
                    .customerId(request.getCustomerId())
                    .providerType(request.getProviderType())
                    .pickupLatitude(request.getPickupLatitude())
                    .pickupLongitude(request.getPickupLongitude())
                    .dropoffLatitude(request.getDropoffLatitude())
                    .dropoffLongitude(request.getDropoffLongitude())
                    .status(request.getStatus())
                    .requestTime(request.getRequestTime())
                    .estimatedFare(request.getEstimatedFare())
                    .actualFare(request.getActualFare())
                    .currency(request.getCurrency())
                    .build();
        });
        doNothing().when(cacheService).set(anyString(), any(), any());
        doNothing().when(idempotencyService).completeOperation(anyString(), any());
        doNothing().when(rideEventService).publishRideAccepted(anyString(), anyString(), anyString(), anyString(), anyString(), anyInt());

        // Act
        CreateRideResponse result = rideService.createRide(createRideRequest);

        // Assert
        assertNotNull(result);
        assertEquals(rideId, result.getRideId());

        verify(idempotencyService).startRideOperation(anyString(), anyString(), any());
        verify(driverProfileRepository).findByAvailableTrue();
        verify(driverProfileRepository).save(any(DriverProfile.class));
        verify(rideRequestRepository).save(any(RideRequest.class));
        verify(cacheService).set(anyString(), any(), any());
    }

    @Test
    void createRide_IdempotencyCheck_Fails() {
        // Arrange
        when(idempotencyService.startRideOperation(anyString(), anyString(), any())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
            rideService.createRide(createRideRequest));

        verify(idempotencyService).startRideOperation(anyString(), anyString(), any());
        verify(rideRequestRepository, never()).save(any());
    }

    @Test
    void getStatus_Success() {
        // Arrange
        when(cacheService.get(anyString(), eq(RideRequest.class)))
                .thenReturn(Optional.of(sampleRideRequest));

        // Act
        RideStatusResponse result = rideService.getStatus(rideId);

        // Assert
        assertNotNull(result);
        assertEquals(rideId, result.getRideId());
        assertEquals(RideStatus.REQUESTED, result.getStatus());

        verify(cacheService).get(eq("ride:" + rideId), eq(RideRequest.class));
    }

    @Test
    void getStatus_NotInCache_FallsBackToDatabase() {
        // Arrange
        when(cacheService.get(anyString(), eq(RideRequest.class)))
                .thenReturn(Optional.empty());
        when(rideRequestRepository.findById(rideId))
                .thenReturn(Optional.of(sampleRideRequest));

        // Act
        RideStatusResponse result = rideService.getStatus(rideId);

        // Assert
        assertNotNull(result);
        assertEquals(rideId, result.getRideId());
        assertEquals(RideStatus.REQUESTED, result.getStatus());

        verify(cacheService).get(eq("ride:" + rideId), eq(RideRequest.class));
        verify(rideRequestRepository).findById(rideId);
    }

    @Test
    void getStatus_RideNotFound_ThrowsException() {
        // Arrange
        when(cacheService.get(anyString(), eq(RideRequest.class)))
                .thenReturn(Optional.empty());
        when(rideRequestRepository.findById(rideId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> 
            rideService.getStatus(rideId));

        verify(rideRequestRepository).findById(rideId);
    }

    @Test
    void cancelRide_Success() {
        // Arrange
        RideRequest activeRide = sampleRideRequest.toBuilder()
                .status(RideStatus.REQUESTED)
                .build();

        when(rideRequestRepository.findById(rideId)).thenReturn(Optional.of(activeRide));
        when(rideRequestRepository.save(any(RideRequest.class))).thenReturn(activeRide);
        doNothing().when(cacheService).set(anyString(), any(), any());

        // Act
        CancelRideResponse result = rideService.cancelRide(rideId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isCancelled());

        verify(rideRequestRepository).save(any(RideRequest.class));
        verify(rideEventService).publishRideCancelled(anyString(), anyString(), anyString());
    }

    @Test
    void cancelRide_AlreadyCompleted_ThrowsException() {
        // Arrange
        RideRequest completedRide = sampleRideRequest.toBuilder()
                .status(RideStatus.COMPLETED)
                .build();

        when(rideRequestRepository.findById(rideId)).thenReturn(Optional.of(completedRide));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
            rideService.cancelRide(rideId));

        verify(rideRequestRepository, never()).save(any());
    }

    @Test
    void completeRide_Success() throws Exception {
        // Arrange
        RideRequest activeRide = sampleRideRequest.toBuilder()
                .status(RideStatus.IN_PROGRESS)
                .driverId(sampleDriver.getDriverId())
                .actualFare(25.50)
                .build();

        when(rideRequestRepository.findById(rideId)).thenReturn(Optional.of(activeRide));
        when(rideRequestRepository.save(any(RideRequest.class))).thenReturn(activeRide);
        when(driverProfileRepository.findById(sampleDriver.getDriverId()))
                .thenReturn(Optional.of(sampleDriver));
        doNothing().when(paymentService).chargeCustomer(any(UUID.class), anyDouble(), anyString());
        doNothing().when(cacheService).set(anyString(), any(), any());
        doNothing().when(rideEventService).publishPaymentProcessed(anyString(), anyString(), anyDouble(), anyString(), anyString());
        doNothing().when(rideEventService).publishRideCompleted(anyString(), anyDouble(), anyInt(), anyDouble(), anyString());

        // Act
        RideCompleteResponse result = rideService.completeRide(rideId);

        // Assert
        assertNotNull(result);
        assertEquals(activeRide.getRideRequestId(), result.getRideId());
        assertEquals(7.42, result.getActualFare(), 0.01);
        assertEquals("USD", result.getCurrency());

        verify(rideRequestRepository).findById(rideId);
        verify(paymentService).chargeCustomer(any(UUID.class), anyDouble(), anyString());
        verify(driverProfileRepository).findById(sampleDriver.getDriverId());
        verify(driverProfileRepository).save(any(DriverProfile.class));
        verify(rideRequestRepository).save(any(RideRequest.class));
        verify(cacheService).set(anyString(), any(), any());
    }

    @Test
    void completeRide_PaymentFailure_ThrowsException() throws Exception {
        // Arrange
        RideRequest activeRide = sampleRideRequest.toBuilder()
                .status(RideStatus.IN_PROGRESS)
                .actualFare(25.50)
                .build();

        when(rideRequestRepository.findById(rideId)).thenReturn(Optional.of(activeRide));
        doThrow(new RuntimeException("Payment failed"))
                .when(paymentService).chargeCustomer(any(UUID.class), anyDouble(), anyString());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            rideService.completeRide(rideId));

        verify(paymentService).chargeCustomer(any(UUID.class), anyDouble(), anyString());
        verify(rideEventService).publishPaymentProcessed(anyString(), isNull(), anyDouble(), eq("USD"), eq("FAILED"));
    }

    @Test
    void getRideFromCache_Success() {
        // Arrange
        when(cacheService.get(eq("ride:" + rideId), eq(RideRequest.class)))
                .thenReturn(Optional.of(sampleRideRequest));

        // Act
        RideRequest result = rideService.getRideFromCache(rideId);

        // Assert
        assertNotNull(result);
        assertEquals(sampleRideRequest.getRideRequestId(), result.getRideRequestId());

        verify(cacheService).get(eq("ride:" + rideId), eq(RideRequest.class));
        verify(rideRequestRepository, never()).findById(any());
    }

    @Test
    void getRideFromCache_NotInCache_FallsBackToDatabase() {
        // Arrange
        when(cacheService.get(eq("ride:" + rideId), eq(RideRequest.class)))
                .thenReturn(Optional.empty());
        when(rideRequestRepository.findById(rideId))
                .thenReturn(Optional.of(sampleRideRequest));

        // Act
        RideRequest result = rideService.getRideFromCache(rideId);

        // Assert
        assertNotNull(result);
        assertEquals(sampleRideRequest.getRideRequestId(), result.getRideRequestId());

        verify(cacheService).get(eq("ride:" + rideId), eq(RideRequest.class));
        verify(rideRequestRepository).findById(rideId);
    }

    @Test
    void getRideFromCache_NotFound_ReturnsNull() {
        // Arrange
        when(cacheService.get(eq("ride:" + rideId), eq(RideRequest.class)))
                .thenReturn(Optional.empty());
        when(rideRequestRepository.findById(rideId))
                .thenReturn(Optional.empty());

        // Act
        RideRequest result = rideService.getRideFromCache(rideId);

        // Assert
        assertNull(result);

        verify(cacheService).get(eq("ride:" + rideId), eq(RideRequest.class));
        verify(rideRequestRepository).findById(rideId);
    }

    @Test
    void completeRide_NotInProgress_ThrowsException() throws Exception {
        // Arrange
        RideRequest pendingRide = sampleRideRequest.toBuilder()
                .status(RideStatus.REQUESTED) // Not IN_PROGRESS
                .build();

        when(rideRequestRepository.findById(rideId)).thenReturn(Optional.of(pendingRide));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
            rideService.completeRide(rideId));

        verify(paymentService, never()).chargeCustomer(any(UUID.class), anyDouble(), anyString());
    }

    @Test
    void completeRide_WithDriver_MakesDriverAvailable() throws Exception {
        // Arrange
        RideRequest rideWithDriver = sampleRideRequest.toBuilder()
                .status(RideStatus.IN_PROGRESS)
                .driverId(sampleDriver.getDriverId())
                .actualFare(30.00)
                .build();

        DriverProfile busyDriver = sampleDriver.toBuilder()
                .available(false)
                .build();

        when(rideRequestRepository.findById(rideId)).thenReturn(Optional.of(rideWithDriver));
        when(rideRequestRepository.save(any(RideRequest.class))).thenReturn(rideWithDriver);
        when(driverProfileRepository.findById(sampleDriver.getDriverId()))
                .thenReturn(Optional.of(busyDriver));
        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(sampleDriver);
        doNothing().when(paymentService).chargeCustomer(any(UUID.class), anyDouble(), anyString());

        // Act
        RideCompleteResponse result = rideService.completeRide(rideId);

        // Assert
        assertNotNull(result);
        assertEquals(7.42, result.getActualFare(), 0.01);

        // Verify driver is made available again
        verify(driverProfileRepository).save(argThat(driver -> driver.getAvailable()));
    }
}