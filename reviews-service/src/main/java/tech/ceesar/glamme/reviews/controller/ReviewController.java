package tech.ceesar.glamme.reviews.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.reviews.dto.CreateReviewRequest;
import tech.ceesar.glamme.reviews.dto.ReviewResponse;
import tech.ceesar.glamme.reviews.service.ReviewService;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    /**
     * Submit a new review for a booking.
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest reviewRequest) {
        ReviewResponse reviewResponse = reviewService.createReview(reviewRequest);
        return ResponseEntity.status(201).body(reviewResponse);
    }

    /**
     * List reviews for a stylist, paginated.
     */
    @GetMapping("/stylist/{stylistId}")
    public PagedResponse<ReviewResponse> getReviewsByStylist(
            @PathVariable("stylistId")UUID stylistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reviewService.getReviewsByStylist(stylistId, page, size);
    }
}
