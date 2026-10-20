package com.habitTracker.HabitTracker.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DiscoverableLoginVerifyRequest(
        @NotBlank String challenge,
        @NotBlank String credentialId,
        @NotBlank String authenticatorData,
        @NotBlank String clientDataJSON,
        @NotBlank String signature,
        @NotBlank String userHandle
) {}
