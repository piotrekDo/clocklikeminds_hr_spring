package com.example.clocklike_portal.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private long userId;
    private String userEmail;
    private String firstName;
    private String lastName;
    private List<String> userRoles;
    private String jwtToken;
    private String jwtExpiresAt;
    private long jwtExpiresAtTimestamp;
}