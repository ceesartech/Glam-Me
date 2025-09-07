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

    @Column(name = "cognito_sub", unique = true)
    private String cognitoSub;

    @Column(name = "username", unique = true)
    private String username;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<Role> roles;

    @Enumerated(EnumType.STRING)
    private SubscriptionType subscriptionType;

    @Column(name = "user_type")
    private String userType; // customer, stylist, admin

    @Column(name = "plan")
    private String plan; // FREE, PRO, PREMIUM

    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    private Instant createdAt;
    private Instant updatedAt;
}
