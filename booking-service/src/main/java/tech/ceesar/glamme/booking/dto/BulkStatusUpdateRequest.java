package tech.ceesar.glamme.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.ceesar.glamme.booking.entity.Booking;

import java.util.List;

/**
 * Bulk status update request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusUpdateRequest {
    private List<String> bookingIds;
    private Booking.Status status;
}
