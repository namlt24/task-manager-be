package com.taskmanager.user.dto;

import java.util.List;

public record UserDto(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String timezone,
        List<String> roles
) {
}
