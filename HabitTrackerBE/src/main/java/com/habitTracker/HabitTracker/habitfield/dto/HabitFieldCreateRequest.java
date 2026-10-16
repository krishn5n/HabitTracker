package com.habitTracker.HabitTracker.habitfield.dto;

import com.habitTracker.HabitTracker.habitfield.HabitFieldType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HabitFieldCreateRequest(
        @Size(max = 100) String fieldName,
        @NotNull HabitFieldType fieldType,
        @Size(max = 30) String unit
) {}
