package com.example.clocklike_portal.security;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AppUserRepository userRepository;
    private final AppUserService appUserService;
    private final JwtService jwtService;
    private final GoogleOauth2LoginService loginService;
    private static final String FAILURE_REDIRECTION = "http://localhost:5173/login-failure";
    private static final String AUTHENTICATED_REDIRECTION = "http://localhost:5173/oauth2/redirect";


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
//        if (googlePrincipal.getHd() == null || !googlePrincipal.getHd().equals("clocklikeminds.com")) {
//            getRedirectStrategy().sendRedirect(request, response, FAILURE_REDIRECTION);
//            log.error("Unauthorized login attempt from " + googlePrincipal.getEmail());
//            return;
//        }

        AppUserEntity appUserEntity = userRepository.findByUserEmailIgnoreCase(googlePrincipal.getEmail())
                .orElseGet(() -> appUserService.registerNewUser(googlePrincipal));

        String jwtToken = jwtService.generateToken(appUserEntity);
        loginService.appendAuthCookie(response, jwtToken);
        getRedirectStrategy().sendRedirect(request, response, AUTHENTICATED_REDIRECTION);
    }

}
