package com.habitTracker.HabitTracker.user.dto;

import com.habitTracker.HabitTracker.user.Gender;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserUpdateRequest(
        @Size(max = 100) String displayName,
        Gender gender,
        LocalDate dateOfBirth,
        BigDecimal weightKg,
        BigDecimal heightCm
) {}
