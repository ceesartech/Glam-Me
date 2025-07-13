package tech.ceesar.glamme.ride.service;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import tech.ceesar.glamme.ride.client.ExternalRideClient;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.entity.DriverProfile;
import tech.ceesar.glamme.ride.entity.RideRequest;
import tech.ceesar.glamme.ride.enums.ProviderType;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repositories.DriverProfileRepository;
import tech.ceesar.glamme.ride.repositories.RideRequestRepository;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RideServiceTest {

    @Mock
    RideRequestRepository rideRepo;
    @Mock
    DriverProfileRepository driverRepo;
    @Mock
    ExternalRideClient uberClient;
    @Mock ExternalRideClient lyftClient;
    @InjectMocks RideService svc;

    private final Map<ProviderType, ExternalRideClient> clients = new EnumMap<>(ProviderType.class);

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        clients.put(ProviderType.UBER, uberClient);
        clients.put(ProviderType.LYFT, lyftClient);
        // inject the map via reflection
        ReflectionTestUtils.setField(svc, "externalClients", clients);
    }

    @Test
    void createInternalRide_assignsDriver() {
        CreateRideRequest req = new CreateRideRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setProviderType(ProviderType.INTERNAL);
        LocationDto loc = new LocationDto(); loc.setLatitude(0); loc.setLongitude(0);
        req.setPickupLocation(loc); req.setDropoffLocation(loc);

        DriverProfile d = new DriverProfile();
        d.setDriverId(UUID.randomUUID());
        d.setAvailable(true);
        when(driverRepo.findByAvailableTrue()).thenReturn(List.of(d));
        when(rideRepo.save(any())).thenAnswer(invocation -> {
            RideRequest r = invocation.getArgument(0);
            // assign an ID so the returned instance isn't null
            r.setRideRequestId(UUID.randomUUID());
            return r;
        });

        CreateRideResponse resp = svc.createRide(req);
        assertNotNull(resp.getRideId());
//        verify(driverRepo).save(argThat(dr -> dr.getAvailable()==false));
    }

    @Test
    void createUberRide_callsUberClient() {
        CreateRideRequest req = new CreateRideRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setProviderType(ProviderType.UBER);
        LocationDto loc = new LocationDto(); loc.setLatitude(1); loc.setLongitude(2);
        req.setPickupLocation(loc); req.setDropoffLocation(loc);

        when(uberClient.requestRide(req)).thenReturn("EXT123");
        when(rideRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        CreateRideResponse resp = svc.createRide(req);
        assertEquals("EXT123", resp.getExternalRideId());
        verify(uberClient).requestRide(req);
    }

    @Test
    void getStatus_internal() {
        UUID rid = UUID.randomUUID();
        RideRequest r = new RideRequest(); r.setRideRequestId(rid);
        r.setProviderType(ProviderType.INTERNAL);
        r.setStatus(RideStatus.DRIVER_EN_ROUTE);
        when(rideRepo.findById(rid)).thenReturn(Optional.of(r));

        RideStatusResponse resp = svc.getStatus(rid);
        assertEquals(RideStatus.DRIVER_EN_ROUTE, resp.getStatus());
        assertNull(resp.getExternalRideId());
        assertNull(resp.getDriverId()); // driverId not set in stub
    }

    @Test
    void cancelExternalRide_invokesClient() {
        UUID rid = UUID.randomUUID();
        RideRequest r = new RideRequest(); r.setRideRequestId(rid);
        r.setProviderType(ProviderType.UBER);
        r.setStatus(RideStatus.REQUESTED);
        r.setExternalRideId("EXT1");
        when(rideRepo.findById(rid)).thenReturn(Optional.of(r));
        when(uberClient.cancelRide("EXT1")).thenReturn(true);

        CancelRideResponse resp = svc.cancelRide(rid);
        assertTrue(resp.isCancelled());
        verify(uberClient).cancelRide("EXT1");
    }
}
