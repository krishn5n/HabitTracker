package com.habitTracker.HabitTracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RegistrationVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String displayName,
        @NotBlank String credentialId,
        @NotBlank String attestationObject,
        @NotBlank String clientDataJSON,
        List<String> transports
) {}
