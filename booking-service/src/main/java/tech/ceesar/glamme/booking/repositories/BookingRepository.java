package tech.ceesar.glamme.booking.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.booking.entity.Booking;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    boolean existsByStylistIdAndScheduledTime(UUID stylistId, LocalDateTime scheduledTime);
}
