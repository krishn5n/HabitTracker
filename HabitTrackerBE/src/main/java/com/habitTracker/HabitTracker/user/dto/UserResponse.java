package com.habitTracker.HabitTracker.user.dto;

import com.habitTracker.HabitTracker.user.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

public record UserResponse(
        Long userSeqId,
        String displayName,
        String email,
        Gender gender,
        LocalDate dateOfBirth,
        int age,
        BigDecimal weightKg,
        BigDecimal heightCm
) {
    public static int ageFrom(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
