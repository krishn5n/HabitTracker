package com.habitTracker.HabitTracker.auth.dto;

import java.util.List;

public record LoginOptionsResponse(
        String challenge,
        long timeout,
        String rpId,
        String userVerification,
        List<AllowCredential> allowCredentials
) {
    public record AllowCredential(String type, String id) {}
}
