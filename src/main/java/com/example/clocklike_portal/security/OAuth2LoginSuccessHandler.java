package com.example.clocklike_portal.security;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.AppUserService;
import com.example.clocklike_portal.mail.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    @Value("${client.baseUrl}")
    private  String clientBaseUrl;
    private final AppUserRepository userRepository;
    private final AppUserService appUserService;
    private final JwtService jwtService;
    private final GoogleOauth2LoginService loginService;
    private final EmailService emailService;
    private String FAILURE_REDIRECTION = clientBaseUrl + "/login-failure";
    private String AUTHENTICATED_REDIRECTION = clientBaseUrl + "/oauth2/redirect";

    @PostConstruct
    void init() {
        FAILURE_REDIRECTION = clientBaseUrl + "/login-failure";
        AUTHENTICATED_REDIRECTION = clientBaseUrl + "/oauth2/redirect";
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        loginService.clearCookies(request, response);
        Optional<GooglePrincipal> principalData = loginService.extractGooglePrincipalData(authentication);
        if (principalData.isEmpty()) {
            getRedirectStrategy().sendRedirect(request, response, FAILURE_REDIRECTION);
            log.warn("Unsuccessful google principal extraction");
            return;
        }
        GooglePrincipal googlePrincipal = principalData.get();
        if (googlePrincipal.getHd() == null || !googlePrincipal.getHd().equals("clocklikeminds.com")) {
            getRedirectStrategy().sendRedirect(request, response, FAILURE_REDIRECTION);
            log.error("Unauthorized login attempt from " + googlePrincipal.getEmail());
            return;
        }

        AppUserEntity appUserEntity;
        Optional<AppUserEntity> userInDataBase = userRepository.findByUserEmailIgnoreCase(googlePrincipal.getEmail());

        if (userInDataBase.isPresent()) {
            appUserEntity = userInDataBase.get();
        } else {
            appUserEntity = appUserService.registerNewUser(googlePrincipal);
            emailService.sendNewEmployeeRegisteredToAdmins(appUserEntity);
        }

        appUserEntity.setImageUrl(googlePrincipal.getPictureUrl());
        userRepository.save(appUserEntity);

        String jwtToken = jwtService.generateToken(appUserEntity);
//        loginService.appendAuthCookie(response, jwtToken);
        System.out.println(AUTHENTICATED_REDIRECTION);

        String authenticatedRedirectionUrl = AUTHENTICATED_REDIRECTION + "?token=" + jwtToken;
        getRedirectStrategy().sendRedirect(request, response, authenticatedRedirectionUrl);
    }

}
