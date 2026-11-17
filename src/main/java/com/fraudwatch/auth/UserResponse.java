package com.fraudwatch.auth;

import com.fraudwatch.user.AppUser;
import com.fraudwatch.user.Role;

public record UserResponse(Long id, String username, Role role) {

    static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
