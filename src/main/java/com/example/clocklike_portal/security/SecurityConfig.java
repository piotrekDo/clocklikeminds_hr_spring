package com.example.clocklike_portal.security;

import com.example.clocklike_portal.appUser.AppUserService;
import com.example.clocklike_portal.error.ErrorEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthorizationFilter jwtAuthenticationFilter;
    private final AppUserService userService;

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final static String USER_AUTHORITY = "user";
    private final static String ADMIN_AUTHORITY = "admin";


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(c -> {
                    CorsConfigurationSource cs = request -> {
                        CorsConfiguration cc = new CorsConfiguration();
                        cc.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
                        cc.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
                        cc.setAllowedHeaders(List.of("Origin", "Content-Type", "X-Auth-Token", "Access-Control-Expose-Header",
                                "Authorization"));
                        return cc;
                    };
                    c.configurationSource(cs);
                })
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers(("/api/v1/test/anon")).permitAll()
                        .requestMatchers(("/api/v1/test/users")).hasAuthority(USER_AUTHORITY)
                        .requestMatchers(("/api/v1/test/admins")).hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers("/api/v1/users/**").hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers("/api/v1/positions/**").hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers("/api/v1/pto/summary").hasAuthority(USER_AUTHORITY)
                        .requestMatchers("/api/v1/pto/request-new").hasAuthority(USER_AUTHORITY)
                        .requestMatchers("/api/v1/pto/byId").hasAuthority(USER_AUTHORITY)
                        .requestMatchers("/api/v1/pto/requests-to-accept").hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers("/api/v1/pto/resolve-request").hasAuthority(ADMIN_AUTHORITY)
                        .anyRequest().denyAll()
                )
                .authenticationProvider(authenticationProvider()).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .permitAll()
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .exceptionHandling(config -> config
                        .accessDeniedHandler(((request, response, accessDeniedException) -> {
                            System.out.println(request.getServletPath());
                            ErrorEntity<String> tokenExpiredError = new ErrorEntity<>(
                                    HttpStatus.FORBIDDEN.value(),
                                    accessDeniedException.getClass().getSimpleName(),
                                    accessDeniedException.getMessage());
                            response.setStatus(403);
                            response.setContentType(APPLICATION_JSON_VALUE);
                            response.getWriter().write(new ObjectMapper().writeValueAsString(tokenExpiredError));
                        }))
                )
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        return authProvider;
    }
}