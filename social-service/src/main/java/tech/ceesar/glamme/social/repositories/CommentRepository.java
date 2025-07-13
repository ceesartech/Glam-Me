package tech.ceesar.glamme.social.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.social.entity.Comment;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByPostId(UUID postId);
    long countByPostId(UUID postId);
}
