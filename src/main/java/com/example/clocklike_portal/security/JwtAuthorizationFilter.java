package com.example.clocklike_portal.security;

import com.example.clocklike_portal.appUser.AppUserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.NoSuchElementException;

import io.jsonwebtoken.security.SignatureException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserService userService;
    private final AuthorizationService authorizationService;

    private final static String AUTH_HEADER = "Authorization";
    private final static String AUTH_HEADER_PREFIX = "Bearer ";


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTH_HEADER);
        final String jwt;
        final String userEmail;
        if (isRequestMissingToken(authHeader)) {
            authorizationService.setNullSecurityContext(request);
            filterChain.doFilter(request, response);
            return;
        }
        try {
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUserName(jwt);
            if (StringUtils.isNotEmpty(userEmail) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    authorizationService.setValidSecurityContext(request, userDetails);
                }
            }
        } catch (SignatureException | ExpiredJwtException | NoSuchElementException e) {
            authorizationService.createJwtExceptionResponse(request, response, e);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRequestMissingToken(String authHeader) {
        return StringUtils.isEmpty(authHeader) || !StringUtils.startsWith(authHeader, AUTH_HEADER_PREFIX);
    }
}
