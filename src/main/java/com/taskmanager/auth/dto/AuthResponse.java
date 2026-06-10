package com.taskmanager.auth.dto;

import com.taskmanager.user.dto.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        UserDto user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs, UserDto user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs, user);
    }
}
