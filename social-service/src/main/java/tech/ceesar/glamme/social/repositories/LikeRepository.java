package tech.ceesar.glamme.social.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.social.entity.Like;

import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByUserIdAndPostId(UUID userId, UUID postId);
    long countByPostId(UUID postId);
}
