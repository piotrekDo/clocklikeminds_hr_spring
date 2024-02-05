package com.example.clocklike_portal.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class GoogleOauth2LoginService {

    String createAuthenticatedUri(String jwt) {
        return String.format("http://localhost:5173/oauth2/redirect?token=%s", jwt
        );
    }

    Optional<GooglePrincipal> extractGooglePrincipalData(Authentication authentication) {
        final String firstName;
        final String lastName;
        final String pictureUrl;
        final String email;
        final String locale;
        final String hd;
        try {
            Object principal = authentication.getPrincipal();
            OAuth2User princpipalOAuth2 = (OAuth2User) principal;
            firstName = (String) princpipalOAuth2.getAttributes().get("given_name");
            lastName = (String) princpipalOAuth2.getAttributes().get("family_name");
            pictureUrl = (String) princpipalOAuth2.getAttributes().get("picture");
            email = (String) princpipalOAuth2.getAttributes().get("email");
            locale = (String) princpipalOAuth2.getAttributes().get("locale");
            hd = (String) princpipalOAuth2.getAttributes().get("hd");
        } catch (Exception e) {
            log.warn(e.getMessage());
            return Optional.empty();
        }
        return Optional.of(new GooglePrincipal(firstName, lastName, pictureUrl, email, locale, hd));
    }

    void clearCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            cookie.setValue("");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }
}
