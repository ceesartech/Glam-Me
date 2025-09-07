package tech.ceesar.glamme.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.ceesar.glamme.image.entity.ImageJob;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageJobRepository extends JpaRepository<ImageJob, String> {

    List<ImageJob> findByUserIdAndStatus(String userId, ImageJob.JobStatus status);

    List<ImageJob> findByStatus(ImageJob.JobStatus status);

    Optional<ImageJob> findByIdAndUserId(String id, String userId);
}
