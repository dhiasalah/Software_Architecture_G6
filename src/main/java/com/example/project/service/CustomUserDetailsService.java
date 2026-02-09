package com.example.project.service;

import com.example.project.entity.User;
import com.example.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

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

        String roleName = "ROLE_" + user.getRole().getName().name();
        System.out.println("=== Loading user: " + username + " with authority: " + roleName + " ===");

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getCredentials().getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}
