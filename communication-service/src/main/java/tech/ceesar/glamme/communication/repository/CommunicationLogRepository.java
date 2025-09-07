package tech.ceesar.glamme.communication.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.communication.entity.CommunicationLog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, UUID> {
    Optional<CommunicationLog> findBySid(String sid);
}
