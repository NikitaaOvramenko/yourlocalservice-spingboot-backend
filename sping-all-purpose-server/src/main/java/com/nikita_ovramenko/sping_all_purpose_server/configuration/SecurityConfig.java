package com.nikita_ovramenko.sping_all_purpose_server.configuration;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nikita_ovramenko.sping_all_purpose_server.common.filter.JwtFilter;

@Configuration
// Turns on @PreAuthorize. The Role enum has always ridden the JWT and
// loadUserByUsername has always emitted ROLE_ADMIN/ROLE_MEMBER; until now nothing
// read either. The admin controllers are the first consumers.
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Value("${spring.app.cors_allowed}")
    private String allowedOrigins;

    SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless bearer-token API: there is no session or browser form to
                // forge, and the token is not sent automatically by the browser.
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(authorize -> authorize
                        // Preflight carries no credentials, so it must pass before any
                        // authorization check or every cross-origin call fails.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public: the customer-facing quote funnel. These are used by
                        // people who are not and never will be logged in.
                        .requestMatchers(HttpMethod.POST, "/api/orgs/*/quotes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/orgs/*/uploads").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orgs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/email/form").permitAll()

                        // Public: obtaining a token in the first place.
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/verify/send").permitAll()
                        // Followed from an email, by someone who by definition has no token.
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify/*").permitAll()

                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // The container's ERROR dispatch. Without this, an unhandled
                        // exception inside an authenticated endpoint is forwarded to
                        // /error, that forward is itself authorized, and the caller sees
                        // 401 "Authentication required" instead of the real 500 -- which
                        // is thoroughly misleading when debugging.
                        .requestMatchers("/error").permitAll()

                        // API documentation. springdoc is switched off entirely by
                        // SWAGGER_ENABLED=false in production, at which point these
                        // routes do not exist and this rule matches nothing.
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**").permitAll()

                        // The admin API. Note this rule is redundant with the
                        // class-level @PreAuthorize on each admin controller, on
                        // purpose: two independent layers, so forgetting one is not a
                        // data breach.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Everything else, including anything added later, needs a token.
                        .anyRequest().authenticated())

                // Without this, an unauthenticated request to a protected route gets 403
                // from the default entry point, which reads as "you are logged in but not
                // allowed" rather than "you did not authenticate".
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpMethod.OPTIONS.name().equals(request.getMethod()) ? 200 : 401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"message\":\"Authentication required\"}");
                        })
                        // Authenticated but not permitted -- a MEMBER reaching for
                        // /api/admin. Deliberately handled here rather than by an
                        // @ExceptionHandler in GlobalExceptionHandler: the advice would
                        // intercept AccessDeniedException before ExceptionTranslationFilter
                        // and collapse "not logged in" (401) into "wrong role" (403).
                        .accessDeniedHandler((request, response, deniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"message\":\"Insufficient permissions\"}");
                        }))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * One CORS definition, driven by the CORS_ALLOWED env var.
     *
     * <p>There used to be a second bean here allowing any origin pattern with
     * credentials enabled, which is exactly the combination that lets any site read
     * authenticated responses. Origins are an explicit allow-list.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
