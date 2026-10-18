package com.habitTracker.HabitTracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;

public record RegistrationVerifyRequest(
        @NotBlank @Email String email,
        @NotBlank String displayName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank String credentialId,
        @NotBlank String attestationObject,
        @NotBlank String clientDataJSON,
        List<String> transports
) {}
