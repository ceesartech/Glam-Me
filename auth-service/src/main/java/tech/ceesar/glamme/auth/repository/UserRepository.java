package tech.ceesar.glamme.auth.repository;

import tech.ceesar.glamme.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByCognitoSub(String cognitoSub);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByCognitoSub(String cognitoSub);
}
