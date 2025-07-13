package tech.ceesar.glamme.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import tech.ceesar.glamme.common.enums.Role;
import tech.ceesar.glamme.common.enums.SubscriptionType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles;

    @Enumerated(EnumType.STRING)
    private SubscriptionType subscriptionType;

    private String oauthProvider;
    private String oauthProviderId;
    private Instant createdAt;
}
