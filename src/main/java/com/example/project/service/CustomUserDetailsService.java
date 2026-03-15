package com.example.project.service;

import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        if (user.getCredentials() == null) {
            throw new UsernameNotFoundException("User credentials not found for: " + username);
        }

        // Charger toutes les PERMISSIONS du rôle de l'utilisateur comme authorities
        List<SimpleGrantedAuthority> authorities = user.getRole().getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getName().name()))
                .collect(Collectors.toList());

        System.out.println("=== Loading user: " + username + " with authorities: " + authorities + " ===");

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getCredentials().getPassword(),
                authorities
        );
    }
}
