package tech.ceesar.glamme.reviews.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.reviews.entity.Review;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByStylistId(UUID stylistId, Pageable page);
}
