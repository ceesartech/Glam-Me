package tech.ceesar.glamme.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ReviewResponse {
    private UUID reviewId;
    private UUID bookingId;
    private UUID stylistId;
    private int rating;
    private String comment;
    private Instant createdAt;
}
