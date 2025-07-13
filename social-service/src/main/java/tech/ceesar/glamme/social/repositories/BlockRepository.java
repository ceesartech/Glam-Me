package tech.ceesar.glamme.social.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.social.entity.Block;

import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {
    boolean existsByBlockerIdAndBlockedId(UUID blocker, UUID blocked);
    Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
