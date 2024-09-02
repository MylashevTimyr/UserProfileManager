package com.example.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import static com.example.user.Role.ADMIN;
import static com.example.user.Role.USER;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private static final String[] WHITE_LIST_URLS = {
            "/api/users/register",
            "/api/users/authenticate",
            "/v2/api-docs",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF for APIs
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(WHITE_LIST_URLS).permitAll()
                        .requestMatchers(GET, "/api/user/**").hasRole(ADMIN.name())
                        .requestMatchers(POST, "/api/user/**").hasAnyRole(ADMIN.name(), USER.name())
                        .requestMatchers(PUT, "/api/user/**").hasAnyRole(ADMIN.name(), USER.name())
                        .requestMatchers(DELETE, "/api/user/**").hasAnyRole(ADMIN.name(), USER.name())
                        .requestMatchers(GET, "/api/user-photo/**").hasAnyRole(ADMIN.name(), USER.name())
                        .requestMatchers(GET, "/api/user-photo/all").hasRole(ADMIN.name())
                        .requestMatchers(POST, "/api/user-photo/**", "/api/user-photo").hasAnyRole(ADMIN.name(), USER.name())
                        .requestMatchers(POST, "/api/user-photo/save").hasAnyRole(ADMIN.name(), USER.name())
                        .requestMatchers(PUT, "/api/user-photo/**").hasAnyRole(ADMIN.name(), USER.name())
                        .requestMatchers(DELETE, "/api/user-photo/**").hasAnyRole(ADMIN.name(), USER.name())
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
                );

        return http.build();
    }
}
