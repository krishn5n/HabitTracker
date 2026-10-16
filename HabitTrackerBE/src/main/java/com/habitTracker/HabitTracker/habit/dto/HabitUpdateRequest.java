package com.habitTracker.HabitTracker.habit.dto;

import jakarta.validation.constraints.Size;

public record HabitUpdateRequest(
        @Size(max = 100) String name,
        @Size(max = 500) String description
) {}
