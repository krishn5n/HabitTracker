package com.habitTracker.HabitTracker.habitfield.dto;

import jakarta.validation.constraints.Size;

public record HabitFieldUpdateRequest(
        @Size(max = 100) String fieldName,
        @Size(max = 30) String unit
) {}
