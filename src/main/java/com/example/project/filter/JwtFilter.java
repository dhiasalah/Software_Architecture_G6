package com.example.project.filter;

import com.example.project.configuration.JwtUtils;
import com.example.project.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        System.out.println("=== JWT FILTER START ===");
        System.out.println("Authorization Header: " + authorizationHeader);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("JWT Token found (first 50 chars): " + jwt.substring(0, Math.min(50, jwt.length())));
            try {
                username = jwtUtils.extractUsername(jwt);
                System.out.println("Username extracted: " + username);
            } catch (Exception e) {
                System.out.println("JWT Token validation FAILED: " + e.getMessage());
            }
        } else {
            System.out.println("No Bearer token found in Authorization header");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            System.out.println("UserDetails loaded with authorities: " + userDetails.getAuthorities());

            if (jwtUtils.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("=== AUTHENTICATION SUCCESS for " + username + " ===");
            } else {
                System.out.println("=== TOKEN VALIDATION FAILED ===");
            }
        }

        System.out.println("=== JWT FILTER END ===");
        filterChain.doFilter(request, response);
    }
}
