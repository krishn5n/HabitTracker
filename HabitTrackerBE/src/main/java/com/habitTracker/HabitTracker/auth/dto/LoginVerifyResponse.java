package com.habitTracker.HabitTracker.auth.dto;

public record LoginVerifyResponse(
        String token,
        Long userSeqId,
        String email
) {}
