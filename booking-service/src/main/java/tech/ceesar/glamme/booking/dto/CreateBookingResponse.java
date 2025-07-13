package tech.ceesar.glamme.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateBookingResponse {
    private UUID bookingId;
    private String checkoutUrl;
}
