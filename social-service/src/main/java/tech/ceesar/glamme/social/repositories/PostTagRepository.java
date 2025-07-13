package tech.ceesar.glamme.social.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.social.entity.PostTag;

import java.util.List;
import java.util.UUID;

public interface PostTagRepository extends JpaRepository<PostTag, UUID> {
    List<PostTag> findByPostId(UUID postId);
}
