package com.nearkirana.backend.config;

import com.nearkirana.backend.security.JsonSecurityErrorHandler;
import com.nearkirana.backend.security.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may call which URL.
 *
 * Every request passes through Spring Security's filter chain BEFORE reaching
 * a controller. For protected URLs it reads "Authorization: Bearer <JWT>",
 * verifies the signature/expiry with our JwtDecoder, and builds the logged-in
 * user (id + role). No valid token -> 401. Wrong role -> 403.
 */
@Configuration
@EnableMethodSecurity // allows @PreAuthorize("hasRole('SHOPKEEPER')") on methods later
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JsonSecurityErrorHandler errorHandler) throws Exception {
        http
                // CSRF protection is for browser cookie sessions. Our API uses Bearer tokens, not cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // STATELESS: no server-side session; every request must carry its own token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no token needed)
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/send-otp",
                                "/api/v1/auth/verify-otp",
                                "/api/v1/auth/cancel-otp",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Role-based areas (used from Stage 7 onwards)
                        .requestMatchers("/api/v1/shopkeeper/**").hasRole("SHOPKEEPER")
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        // Everything else: any logged-in user
                        .anyRequest().authenticated())
                // Validate JWT Bearer tokens on incoming requests
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));

        return http.build();
    }

    /** Turns the token's "role": "SHOPKEEPER" claim into the Spring authority "ROLE_SHOPKEEPER". */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenService.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
