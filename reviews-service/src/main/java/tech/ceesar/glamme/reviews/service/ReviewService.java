package tech.ceesar.glamme.reviews.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.reviews.dto.CreateReviewRequest;
import tech.ceesar.glamme.reviews.dto.ReviewResponse;
import tech.ceesar.glamme.reviews.entity.Review;
import tech.ceesar.glamme.reviews.repository.ReviewRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public ReviewResponse createReview(CreateReviewRequest reviewRequest) {
        if (reviewRepository.existsById(reviewRequest.getBookingId())) {
            throw new BadRequestException("Review already exists for booking: " + reviewRequest.getBookingId());
        }

        Review review = Review.builder()
                .bookingId(reviewRequest.getBookingId())
                .stylistId(reviewRequest.getStylistId())
                .rating(reviewRequest.getRating())
                .comment(reviewRequest.getComment())
                .build();

        review = reviewRepository.save(review);
        return map(review);
    }

    public PagedResponse<ReviewResponse> getReviewsByStylist(UUID stylistId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviews = reviewRepository.findByStylistId(stylistId, pageable);
        var dtos = reviews.stream().map(this::map).toList();

        return new PagedResponse<>(
                dtos,
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalElements(),
                reviews.getTotalPages(),
                reviews.isLast()
        );
    }

    private ReviewResponse map(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getBookingId(),
                review.getStylistId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
