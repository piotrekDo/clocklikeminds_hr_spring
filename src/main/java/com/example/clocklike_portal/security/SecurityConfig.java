package com.example.clocklike_portal.security;

import com.example.clocklike_portal.appUser.AppUserService;
import com.example.clocklike_portal.error.ErrorEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
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
    public static final String API_VERSION = "/api/v1";

    private final JwtAuthorizationFilter jwtAuthenticationFilter;
    private final AppUserService userService;

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    public final static String USER_AUTHORITY = "user";
    public final static String ADMIN_AUTHORITY = "admin";
    public final static String SUPERVISOR_AUTHORITY = "supervisor";


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(c -> {
                    CorsConfigurationSource cs = request -> {
                        CorsConfiguration cc = new CorsConfiguration();
                        cc.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "https://clocklikeminds-hr-react.vercel.app"));
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
                        .requestMatchers((API_VERSION + "/test/anon")).permitAll()
                        .requestMatchers((API_VERSION + "/health/**")).permitAll()
                        .requestMatchers((API_VERSION + "/meta")).hasAuthority(USER_AUTHORITY)
                        .requestMatchers((API_VERSION + "/test/users")).hasAuthority(USER_AUTHORITY)
                        .requestMatchers((API_VERSION + "/test/admins")).hasAnyAuthority(ADMIN_AUTHORITY)
                        .requestMatchers((API_VERSION + "/settings/**")).hasAnyAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(API_VERSION + "/users/{id}").hasAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/users/**").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/positions/**").hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(API_VERSION + "/dashboard/supervisor").hasAnyAuthority(SUPERVISOR_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/summary").hasAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/requests-for-year").hasAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/all-requests-for-user").hasAnyAuthority(SUPERVISOR_AUTHORITY, ADMIN_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/requests-for-user-calendar").hasAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/request-new").hasAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/byId").hasAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/withdraw").hasAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/unresolved-by-acceptor").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/requests-for-supervisor-calendar").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/requests-for-supervisor-calendar-by-employees").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/requests-by-acceptor").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/resolve-request").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/new-saturday-holiday").hasAnyAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/holidays-on-saturday-admin").hasAnyAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/holiday-on-saturday-by-users").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/by-criteria-admin").hasAnyAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/by-criteria-supervisor").hasAnyAuthority(SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/generate-pdf").hasAnyAuthority(USER_AUTHORITY)
                        .requestMatchers(API_VERSION + "/pto/resend-request-by-mail").hasAnyAuthority(ADMIN_AUTHORITY, SUPERVISOR_AUTHORITY)
                        .requestMatchers(API_VERSION + "/report/generate-creative-work-report-template").hasAnyAuthority(USER_AUTHORITY)
                        .anyRequest().denyAll()
                )
                .authenticationProvider(authenticationProvider()).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .permitAll()
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .exceptionHandling(config -> config
                        .accessDeniedHandler(((request, response, accessDeniedException) -> {
                            ErrorEntity<String> tokenExpiredError = new ErrorEntity<>(
                                    HttpStatus.FORBIDDEN.value(),
                                    accessDeniedException.getClass().getSimpleName(),
                                    accessDeniedException.getMessage());
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
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