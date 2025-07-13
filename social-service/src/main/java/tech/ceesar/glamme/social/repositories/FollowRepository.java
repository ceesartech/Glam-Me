package tech.ceesar.glamme.social.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.social.entity.Follow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollowerIdAndFollowedId(UUID follower, UUID followed);
    List<Follow> findByFollowerId(UUID followerId);
    Optional<Follow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
}
