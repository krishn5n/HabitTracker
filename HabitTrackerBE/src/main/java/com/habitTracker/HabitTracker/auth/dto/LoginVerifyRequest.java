package com.habitTracker.HabitTracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String credentialId,
        @NotBlank String authenticatorData,
        @NotBlank String clientDataJSON,
        @NotBlank String signature,
        String userHandle
) {}
