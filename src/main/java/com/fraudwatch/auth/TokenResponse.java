package com.fraudwatch.auth;

import com.fraudwatch.user.Role;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, String username, Role role) {
}
