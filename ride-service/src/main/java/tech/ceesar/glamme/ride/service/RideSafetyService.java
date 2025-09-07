package tech.ceesar.glamme.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.ride.controller.WebSocketController;
import tech.ceesar.glamme.ride.dto.*;
import tech.ceesar.glamme.ride.entity.Ride;
import tech.ceesar.glamme.ride.enums.RideStatus;
import tech.ceesar.glamme.ride.repository.RideRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive safety service for ride protection and emergency features
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RideSafetyService {

    private final RideRepository rideRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EventPublisher eventPublisher;
    private final WebSocketController webSocketController;

    // Emergency contacts cache
    private final Map<String, EmergencyContacts> emergencyContactsCache = new ConcurrentHashMap<>();

    // SOS alerts tracking
    private final Map<String, SOSAlert> activeSOSAlerts = new ConcurrentHashMap<>();

    /**
     * Trigger SOS emergency alert
     */
    @Transactional
    public SOSResponse triggerSOS(String rideId, String userId, String alertType, String message, LocationDto location) {
        log.warn("🚨 SOS ALERT triggered for ride: {}, user: {}, type: {}", rideId, userId, alertType);

        try {
            // Get ride details
            Optional<tech.ceesar.glamme.ride.entity.RideRequest> rideOpt = rideRepository.findByRideId(rideId);
            if (!rideOpt.isPresent()) {
                throw new IllegalArgumentException("Ride not found: " + rideId);
            }

            tech.ceesar.glamme.ride.entity.RideRequest ride = rideOpt.get();
            String customerId = ride.getCustomerId().toString();
            String driverId = ride.getDriverId().toString();

            // Create SOS alert
            SOSAlert sosAlert = new SOSAlert(
                    rideId,
                    userId,
                    alertType,
                    message,
                    location,
                    LocalDateTime.now(),
                    "ACTIVE"
            );

            activeSOSAlerts.put(rideId, sosAlert);

            // Notify emergency contacts
            notifyEmergencyContacts(rideId, customerId, sosAlert);

            // Notify driver
            if (driverId != null) {
                notifyDriverOfEmergency(driverId, sosAlert);
            }

            // Broadcast to emergency monitoring systems
            messagingTemplate.convertAndSend("/topic/emergency/alerts", sosAlert);
            messagingTemplate.convertAndSend("/topic/admin/emergency", sosAlert);

            // Publish emergency event
            eventPublisher.publishEvent("glamme-bus",
                    Map.of(
                            "eventType", "EMERGENCY_SOS",
                            "rideId", rideId,
                            "userId", userId,
                            "alertType", alertType,
                            "message", message,
                            "latitude", location != null ? String.valueOf(location.getLatitude()) : "0",
                            "longitude", location != null ? String.valueOf(location.getLongitude()) : "0",
                            "timestamp", LocalDateTime.now().toString()
                    ));

            // Auto-call emergency services for critical alerts
            if ("CRITICAL".equals(alertType) || "MEDICAL".equals(alertType)) {
                triggerEmergencyServices(rideId, sosAlert);
            }

            log.info("✅ SOS alert processed for ride: {}", rideId);

            return new SOSResponse(true, "Emergency alert sent successfully", sosAlert);

        } catch (Exception e) {
            log.error("❌ Failed to process SOS alert for ride: {}", rideId, e);
            return new SOSResponse(false, "Failed to send emergency alert: " + e.getMessage(), null);
        }
    }

    /**
     * Resolve SOS emergency alert
     */
    @Transactional
    public boolean resolveSOS(String rideId, String resolvedBy, String resolutionNotes) {
        log.info("🔧 Resolving SOS alert for ride: {} by: {}", rideId, resolvedBy);

        try {
            SOSAlert alert = activeSOSAlerts.get(rideId);
            if (alert == null) {
                log.warn("No active SOS alert found for ride: {}", rideId);
                return false;
            }

            alert.setStatus("RESOLVED");
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolvedBy(resolvedBy);
            alert.setResolutionNotes(resolutionNotes);

            // Remove from active alerts
            activeSOSAlerts.remove(rideId);

            // Notify all parties
            notifySOSResolution(rideId, alert);

            // Publish resolution event
            eventPublisher.publishEvent("glamme-bus",
                    Map.of(
                            "eventType", "EMERGENCY_RESOLVED",
                            "rideId", rideId,
                            "resolvedBy", resolvedBy,
                            "resolutionNotes", resolutionNotes,
                            "timestamp", LocalDateTime.now().toString()
                    ));

            log.info("✅ SOS alert resolved for ride: {}", rideId);
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to resolve SOS alert for ride: {}", rideId, e);
            return false;
        }
    }

    /**
     * Set emergency contacts for a user
     */
    @Transactional
    public boolean setEmergencyContacts(String userId, List<EmergencyContact> contacts) {
        log.info("📞 Setting emergency contacts for user: {}", userId);

        try {
            EmergencyContacts emergencyContacts = new EmergencyContacts(userId, contacts, LocalDateTime.now());
            emergencyContactsCache.put(userId, emergencyContacts);

            log.info("✅ Emergency contacts set for user: {} ({} contacts)", userId, contacts.size());
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to set emergency contacts for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Get emergency contacts for a user
     */
    public List<EmergencyContact> getEmergencyContacts(String userId) {
        EmergencyContacts contacts = emergencyContactsCache.get(userId);
        return contacts != null ? contacts.getContacts() : List.of();
    }

    /**
     * Enable ride sharing for a ride
     */
    @Transactional
    public RideSharingResponse enableRideSharing(String rideId, Integer maxPassengers, BigDecimal fareSplit) {
        log.info("🚗 Enabling ride sharing for ride: {} (max: {}, split: {})",
                rideId, maxPassengers, fareSplit);

        try {
            Optional<tech.ceesar.glamme.ride.entity.RideRequest> rideOpt = rideRepository.findByRideId(rideId);
            if (!rideOpt.isPresent()) {
                return new RideSharingResponse(false, "Ride not found", null);
            }

            tech.ceesar.glamme.ride.entity.RideRequest ride = rideOpt.get();

            // Create ride sharing session
            RideSharingSession session = new RideSharingSession(
                    rideId,
                    ride.getCustomerId().toString(),
                    maxPassengers,
                    fareSplit,
                    List.of(ride.getCustomerId().toString()), // Initial passenger
                    "ACTIVE",
                    LocalDateTime.now()
            );

            // Broadcast ride sharing availability
            messagingTemplate.convertAndSend("/topic/ridesharing/available", session);

            log.info("✅ Ride sharing enabled for ride: {}", rideId);
            return new RideSharingResponse(true, "Ride sharing enabled", session);

        } catch (Exception e) {
            log.error("❌ Failed to enable ride sharing for ride: {}", rideId, e);
            return new RideSharingResponse(false, "Failed to enable ride sharing: " + e.getMessage(), null);
        }
    }

    /**
     * Join a ride sharing session
     */
    @Transactional
    public boolean joinRideSharing(String rideId, String passengerId, LocationDto pickupLocation) {
        log.info("👥 Passenger {} joining ride sharing for ride: {}", passengerId, rideId);

        try {
            // In a real implementation, this would check availability and update the ride
            // For now, just log and return success

            // Notify ride owner
            messagingTemplate.convertAndSendToUser(
                    passengerId,
                    "/queue/ridesharing/joined",
                    Map.of("rideId", rideId, "status", "JOINED")
            );

            log.info("✅ Passenger {} joined ride sharing for ride: {}", passengerId, rideId);
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to join ride sharing for ride: {}, passenger: {}", rideId, passengerId, e);
            return false;
        }
    }

    /**
     * Send safety check-in to emergency contacts
     */
    public boolean sendSafetyCheckIn(String rideId, String userId, LocationDto location) {
        log.info("🛡️ Sending safety check-in for ride: {}, user: {}", rideId, userId);

        try {
            List<EmergencyContact> contacts = getEmergencyContacts(userId);

            for (EmergencyContact contact : contacts) {
                // In a real implementation, this would send SMS/email
                log.info("Safety check-in sent to: {} ({})", contact.getName(), contact.getPhoneNumber());
            }

            // Publish safety check-in event
            eventPublisher.publishEvent("glamme-bus",
                    Map.of(
                            "eventType", "SAFETY_CHECKIN",
                            "rideId", rideId,
                            "userId", userId,
                            "latitude", String.valueOf(location.getLatitude()),
                            "longitude", String.valueOf(location.getLongitude()),
                            "timestamp", LocalDateTime.now().toString()
                    ));

            return true;

        } catch (Exception e) {
            log.error("❌ Failed to send safety check-in for ride: {}", rideId, e);
            return false;
        }
    }

    /**
     * Monitor ride safety and trigger alerts if needed
     */
    public void monitorRideSafety(String rideId) {
        log.info("🔍 Monitoring safety for ride: {}", rideId);

        try {
            Optional<tech.ceesar.glamme.ride.entity.RideRequest> rideOpt = rideRepository.findByRideId(rideId);
            if (!rideOpt.isPresent()) {
                log.warn("Ride not found for safety monitoring: {}", rideId);
                return;
            }

            tech.ceesar.glamme.ride.entity.RideRequest ride = rideOpt.get();
            LocalDateTime now = LocalDateTime.now();

            // Check for overdue rides
            // Safety check for long rides - simplified since getStartedAt() is not available in RideRequest
            if (ride.getStatus().equals(RideStatus.STARTED.toString())) {
                // TODO: Add duration tracking to RideRequest entity
                log.debug("Ride {} is in STARTED status", rideId);
            }

            // Check for rides without updates
            // Check for rides without updates - simplified since getUpdatedAt() is not available
            // TODO: Add update tracking to RideRequest entity
            log.debug("Ride safety monitoring active for ride {}", rideId);

        } catch (Exception e) {
            log.error("❌ Error monitoring safety for ride: {}", rideId, e);
        }
    }

    /**
     * Report suspicious activity
     */
    @Transactional
    public boolean reportSuspiciousActivity(String rideId, String reporterId, String activityType, String description, LocationDto location) {
        log.warn("🚨 Suspicious activity reported for ride: {} by: {}", rideId, reporterId);

        try {
            // Create incident report
            IncidentReport report = new IncidentReport(
                    rideId,
                    reporterId,
                    activityType,
                    description,
                    location,
                    LocalDateTime.now()
            );

            // Notify authorities and monitoring systems
            messagingTemplate.convertAndSend("/topic/incidents", report);
            messagingTemplate.convertAndSend("/topic/admin/incidents", report);

            // Publish incident event
            eventPublisher.publishEvent("glamme-bus",
                    Map.of(
                            "eventType", "INCIDENT_REPORTED",
                            "rideId", rideId,
                            "reporterId", reporterId,
                            "activityType", activityType,
                            "description", description,
                            "latitude", String.valueOf(location.getLatitude()),
                            "longitude", String.valueOf(location.getLongitude()),
                            "timestamp", LocalDateTime.now().toString()
                    ));

            log.info("✅ Suspicious activity reported for ride: {}", rideId);
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to report suspicious activity for ride: {}", rideId, e);
            return false;
        }
    }

    /**
     * Get active SOS alerts
     */
    public List<SOSAlert> getActiveSOSAlerts() {
        return List.copyOf(activeSOSAlerts.values());
    }

    // Private helper methods

    private void notifyEmergencyContacts(String rideId, String customerId, SOSAlert alert) {
        List<EmergencyContact> contacts = getEmergencyContacts(customerId);

        for (EmergencyContact contact : contacts) {
            log.info("📞 Notifying emergency contact: {} for ride: {}", contact.getName(), rideId);

            // In a real implementation, this would send SMS/email
            // For now, just log the notification
            messagingTemplate.convertAndSend("/topic/emergency/contacts",
                    Map.of(
                            "contact", contact,
                            "alert", alert,
                            "rideId", rideId
                    ));
        }
    }

    private void notifyDriverOfEmergency(String driverId, SOSAlert alert) {
        log.info("📢 Notifying driver {} of emergency for ride: {}", driverId, alert.getRideId());

        messagingTemplate.convertAndSendToUser(
                driverId,
                "/queue/emergency",
                alert
        );
    }

    private void triggerEmergencyServices(String rideId, SOSAlert alert) {
        log.warn("🚨 Triggering emergency services for ride: {}", rideId);

        // In a real implementation, this would:
        // 1. Call 911/police
        // 2. Send location to emergency services
        // 3. Alert nearby drivers and authorities

        messagingTemplate.convertAndSend("/topic/emergency/services", alert);
    }

    private void notifySOSResolution(String rideId, SOSAlert alert) {
        log.info("✅ Notifying resolution of SOS alert for ride: {}", rideId);

        // Notify customer
        messagingTemplate.convertAndSendToUser(
                alert.getUserId(),
                "/queue/emergency/resolved",
                alert
        );

        // Notify emergency contacts
        List<EmergencyContact> contacts = getEmergencyContacts(alert.getUserId());
        for (EmergencyContact contact : contacts) {
            messagingTemplate.convertAndSend("/topic/emergency/contacts/resolved",
                    Map.of("contact", contact, "alert", alert));
        }

        // Broadcast resolution
        messagingTemplate.convertAndSend("/topic/emergency/resolved", alert);
    }

    // DTO classes for safety features

    public static class SOSResponse {
        private boolean success;
        private String message;
        private SOSAlert alert;

        public SOSResponse(boolean success, String message, SOSAlert alert) {
            this.success = success;
            this.message = message;
            this.alert = alert;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public SOSAlert getAlert() { return alert; }
    }

    public static class SOSAlert {
        private String rideId;
        private String userId;
        private String alertType;
        private String message;
        private LocationDto location;
        private LocalDateTime triggeredAt;
        private String status;
        private LocalDateTime resolvedAt;
        private String resolvedBy;
        private String resolutionNotes;

        public SOSAlert(String rideId, String userId, String alertType, String message,
                       LocationDto location, LocalDateTime triggeredAt, String status) {
            this.rideId = rideId;
            this.userId = userId;
            this.alertType = alertType;
            this.message = message;
            this.location = location;
            this.triggeredAt = triggeredAt;
            this.status = status;
        }

        // Getters and setters
        public String getRideId() { return rideId; }
        public void setRideId(String rideId) { this.rideId = rideId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public LocationDto getLocation() { return location; }
        public void setLocation(LocationDto location) { this.location = location; }

        public LocalDateTime getTriggeredAt() { return triggeredAt; }
        public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

        public String getResolvedBy() { return resolvedBy; }
        public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

        public String getResolutionNotes() { return resolutionNotes; }
        public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    }

    public static class EmergencyContacts {
        private String userId;
        private List<EmergencyContact> contacts;
        private LocalDateTime lastUpdated;

        public EmergencyContacts(String userId, List<EmergencyContact> contacts, LocalDateTime lastUpdated) {
            this.userId = userId;
            this.contacts = contacts;
            this.lastUpdated = lastUpdated;
        }

        // Getters
        public String getUserId() { return userId; }
        public List<EmergencyContact> getContacts() { return contacts; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    public static class EmergencyContact {
        private String name;
        private String phoneNumber;
        private String email;
        private String relationship;

        public EmergencyContact(String name, String phoneNumber, String email, String relationship) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.relationship = relationship;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRelationship() { return relationship; }
        public void setRelationship(String relationship) { this.relationship = relationship; }
    }

    public static class RideSharingResponse {
        private boolean success;
        private String message;
        private RideSharingSession session;

        public RideSharingResponse(boolean success, String message, RideSharingSession session) {
            this.success = success;
            this.message = message;
            this.session = session;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public RideSharingSession getSession() { return session; }
    }

    public static class RideSharingSession {
        private String rideId;
        private String ownerId;
        private int maxPassengers;
        private BigDecimal fareSplit;
        private List<String> passengerIds;
        private String status;
        private LocalDateTime createdAt;

        public RideSharingSession(String rideId, String ownerId, int maxPassengers,
                                BigDecimal fareSplit, List<String> passengerIds,
                                String status, LocalDateTime createdAt) {
            this.rideId = rideId;
            this.ownerId = ownerId;
            this.maxPassengers = maxPassengers;
            this.fareSplit = fareSplit;
            this.passengerIds = passengerIds;
            this.status = status;
            this.createdAt = createdAt;
        }

        // Getters
        public String getRideId() { return rideId; }
        public String getOwnerId() { return ownerId; }
        public int getMaxPassengers() { return maxPassengers; }
        public BigDecimal getFareSplit() { return fareSplit; }
        public List<String> getPassengerIds() { return passengerIds; }
        public String getStatus() { return status; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class IncidentReport {
        private String rideId;
        private String reporterId;
        private String activityType;
        private String description;
        private LocationDto location;
        private LocalDateTime reportedAt;

        public IncidentReport(String rideId, String reporterId, String activityType,
                            String description, LocationDto location, LocalDateTime reportedAt) {
            this.rideId = rideId;
            this.reporterId = reporterId;
            this.activityType = activityType;
            this.description = description;
            this.location = location;
            this.reportedAt = reportedAt;
        }

        // Getters
        public String getRideId() { return rideId; }
        public String getReporterId() { return reporterId; }
        public String getActivityType() { return activityType; }
        public String getDescription() { return description; }
        public LocationDto getLocation() { return location; }
        public LocalDateTime getReportedAt() { return reportedAt; }
    }
}
