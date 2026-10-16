package com.habitTracker.HabitTracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginOptionsRequest(
        @NotBlank @Email String email
) {}
