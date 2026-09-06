package com.nikita_ovramenko.sping_all_purpose_server.common.filter;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nikita_ovramenko.sping_all_purpose_server.common.service.JwtService;
import com.nikita_ovramenko.sping_all_purpose_server.app_user.service.AuthSessionService;
import io.jsonwebtoken.JwtException;

import org.springframework.context.annotation.Lazy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuthSessionService sessions;

    public JwtFilter(JwtService jwtService, @Lazy UserDetailsService userDetailsService, AuthSessionService sessions) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate JWT and set authentication - only catch JWT-related exceptions
        try {
            final String jwt = authHeader.substring(7);
            final JwtService.TokenIdentity identity = jwtService.readAccessToken(jwt);
            final String userEmail = identity.email();
            sessions.requireActive(identity.sessionId(), userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (!userDetails.isEnabled()) {
                    throw new JwtException("Account unavailable");
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (UsernameNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\": 401, \"message\": \"User not found\"}");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\"Session expired or invalid\"}");
            return;
        }

        // Continue filter chain OUTSIDE try-catch so controller errors aren't caught as 401
        filterChain.doFilter(request, response);
    }

}
