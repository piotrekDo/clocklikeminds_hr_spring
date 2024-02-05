package com.example.clocklike_portal.security;

import java.security.Key;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.UserDetailsAdapter;
import com.example.clocklike_portal.appUser.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${token.secret.key}")
    String jwtSecretKey;

    @Value("${token.expirationms}")
    Long jwtExpirationMs;

    String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    String generateToken(AppUserEntity appUser) {
        UserDetailsAdapter userDetails = new UserDetailsAdapter(appUser);
        Long appUserId = appUser.getAppUserId();
        List<String> authorities = appUser.getUserRoles().stream().map(UserRole::getRoleName).toList();
        return generateToken(Map.of(
                "roles", authorities,
                "userId", appUserId),
                userDetails);
    }

    boolean isTokenValid(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        TimeZone timeZone = TimeZone.getDefault();
        int timezoneOffsetInMinutes = timeZone.getOffset(new Date().getTime()) / (60 * 1000);
        long timezoneOffsetInMilliseconds = timezoneOffsetInMinutes * 60 * 1000;
        System.out.println(timezoneOffsetInMilliseconds);
        System.out.println(jwtExpirationMs);
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis() +timezoneOffsetInMilliseconds ))
                .setExpiration(new Date(System.currentTimeMillis() + timezoneOffsetInMilliseconds + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
