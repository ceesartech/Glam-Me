package tech.ceesar.glamme.social.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.social.entity.Post;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    Page<Post> findByUserIdIn(List<UUID> userIds, Pageable pageable);
    long countByOriginalPostId(UUID originalPostId);
}
