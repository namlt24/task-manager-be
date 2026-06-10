package com.taskmanager.label.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LabelRequest(
        @NotBlank @Size(max = 50) String name,
        @Size(max = 20) String color
) {
}
