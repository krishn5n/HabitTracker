package com.habitTracker.HabitTracker.habitfield.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record HabitFieldUpdateRequest(
        @Size(max = 100) String fieldName,
        @Size(max = 30) String unit,
        BigDecimal goalValue
) {}
