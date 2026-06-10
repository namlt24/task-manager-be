package com.taskmanager.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 150) String fullName,
        @Size(max = 60) String timezone,
        @Size(max = 500) String avatarUrl
) {
}
