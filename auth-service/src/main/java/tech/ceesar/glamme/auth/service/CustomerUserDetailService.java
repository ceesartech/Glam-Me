package tech.ceesar.glamme.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.auth.entity.User;
import tech.ceesar.glamme.auth.repository.UserRepository;
import tech.ceesar.glamme.common.enums.Role;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return buildSpringUser(user);
    }

    public UserDetails loadUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return buildSpringUser(user);
    }

    private UserDetails buildSpringUser(User user) {
        var authorities = user.getRoles().stream()
                .map(Role::name)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword() == null ? "" : user.getPassword(),
                authorities
        );
    }
}
