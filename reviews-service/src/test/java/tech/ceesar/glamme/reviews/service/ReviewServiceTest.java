package tech.ceesar.glamme.reviews.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tech.ceesar.glamme.common.dto.PagedResponse;
import tech.ceesar.glamme.common.exception.BadRequestException;
import tech.ceesar.glamme.reviews.dto.CreateReviewRequest;
import tech.ceesar.glamme.reviews.dto.ReviewResponse;
import tech.ceesar.glamme.reviews.entity.Review;
import tech.ceesar.glamme.reviews.repository.ReviewRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    @Mock ReviewRepository reviewRepo;
    @InjectMocks ReviewService service;

    private final UUID bookingId = UUID.randomUUID();
    private final UUID stylistId = UUID.randomUUID();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createReview_success() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(bookingId);
        req.setStylistId(stylistId);
        req.setRating(4);
        req.setComment("Great!");

        when(reviewRepo.existsById(bookingId)).thenReturn(false);
        Review saved = Review.builder()
                .reviewId(UUID.randomUUID())
                .bookingId(bookingId)
                .stylistId(stylistId)
                .rating(4)
                .comment("Great!")
                .createdAt(Instant.now())
                .build();
        when(reviewRepo.save(any(Review.class))).thenReturn(saved);

        ReviewResponse resp = service.createReview(req);
        assertEquals(saved.getReviewId(), resp.getReviewId());
        assertEquals(4, resp.getRating());
        assertEquals("Great!", resp.getComment());
    }

    @Test
    void createReview_duplicate_throws() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(bookingId);
        req.setStylistId(stylistId);
        req.setRating(5);

        when(reviewRepo.existsById(bookingId)).thenReturn(true);
        assertThrows(BadRequestException.class,
                () -> service.createReview(req));
    }

    @Test
    void getReviewsByStylist_returnsPaged() {
        Review r1 = Review.builder()
                .reviewId(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .stylistId(stylistId)
                .rating(5)
                .comment("A")
                .createdAt(Instant.now())
                .build();
        List<Review> list = List.of(r1);
        Page<Review> page = new PageImpl<>(list, PageRequest.of(0, 1), 1);

        when(reviewRepo.findByStylistId(
                eq(stylistId), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<ReviewResponse> resp =
                service.getReviewsByStylist(stylistId, 0, 1);

        assertEquals(1, resp.getContent().size());
        assertEquals("A", resp.getContent().get(0).getComment());
        assertTrue(resp.isLast());
    }

    @Test
    void createReview_withMinimumRating_Success() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(bookingId);
        req.setStylistId(stylistId);
        req.setRating(1); // Minimum rating
        req.setComment("Needs improvement");

        when(reviewRepo.existsById(bookingId)).thenReturn(false);
        Review saved = Review.builder()
                .reviewId(UUID.randomUUID())
                .bookingId(bookingId)
                .stylistId(stylistId)
                .rating(1)
                .comment("Needs improvement")
                .createdAt(Instant.now())
                .build();
        when(reviewRepo.save(any(Review.class))).thenReturn(saved);

        ReviewResponse resp = service.createReview(req);
        assertEquals(1, resp.getRating());
        assertEquals("Needs improvement", resp.getComment());
    }

    @Test
    void createReview_withMaximumRating_Success() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(bookingId);
        req.setStylistId(stylistId);
        req.setRating(5); // Maximum rating
        req.setComment("Absolutely amazing service!");

        when(reviewRepo.existsById(bookingId)).thenReturn(false);
        Review saved = Review.builder()
                .reviewId(UUID.randomUUID())
                .bookingId(bookingId)
                .stylistId(stylistId)
                .rating(5)
                .comment("Absolutely amazing service!")
                .createdAt(Instant.now())
                .build();
        when(reviewRepo.save(any(Review.class))).thenReturn(saved);

        ReviewResponse resp = service.createReview(req);
        assertEquals(5, resp.getRating());
        assertEquals("Absolutely amazing service!", resp.getComment());
    }

    @Test
    void createReview_withEmptyComment_Success() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setBookingId(bookingId);
        req.setStylistId(stylistId);
        req.setRating(4);
        req.setComment(""); // Empty comment

        when(reviewRepo.existsById(bookingId)).thenReturn(false);
        Review saved = Review.builder()
                .reviewId(UUID.randomUUID())
                .bookingId(bookingId)
                .stylistId(stylistId)
                .rating(4)
                .comment("")
                .createdAt(Instant.now())
                .build();
        when(reviewRepo.save(any(Review.class))).thenReturn(saved);

        ReviewResponse resp = service.createReview(req);
        assertEquals(4, resp.getRating());
        assertEquals("", resp.getComment());
    }

    @Test
    void getReviewsByStylist_emptyResults_ReturnsEmptyPage() {
        Page<Review> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(reviewRepo.findByStylistId(eq(stylistId), any(Pageable.class)))
                .thenReturn(emptyPage);

        PagedResponse<ReviewResponse> resp = service.getReviewsByStylist(stylistId, 0, 10);

        assertEquals(0, resp.getContent().size());
        assertTrue(resp.isLast());
        assertTrue(resp.isFirst());
        assertEquals(0, resp.getTotalElements());
    }

    @Test
    void getReviewsByStylist_multipleReviews_ReturnsSorted() {
        Review r1 = Review.builder()
                .reviewId(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .stylistId(stylistId)
                .rating(5)
                .comment("Excellent")
                .createdAt(Instant.now().minusSeconds(100))
                .build();
        
        Review r2 = Review.builder()
                .reviewId(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .stylistId(stylistId)
                .rating(4)
                .comment("Good")
                .createdAt(Instant.now())
                .build();

        List<Review> reviews = List.of(r2, r1); // r2 is more recent
        Page<Review> page = new PageImpl<>(reviews, PageRequest.of(0, 10), 2);

        when(reviewRepo.findByStylistId(eq(stylistId), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<ReviewResponse> resp = service.getReviewsByStylist(stylistId, 0, 10);

        assertEquals(2, resp.getContent().size());
        assertEquals("Good", resp.getContent().get(0).getComment()); // More recent first
        assertEquals("Excellent", resp.getContent().get(1).getComment());
        assertEquals(2, resp.getTotalElements());
    }
}
