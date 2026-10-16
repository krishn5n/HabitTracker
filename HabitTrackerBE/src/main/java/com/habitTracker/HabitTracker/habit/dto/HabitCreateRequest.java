package com.habitTracker.HabitTracker.habit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HabitCreateRequest(
        @NotNull Long userSeqId,
        @NotNull @Size(max = 100) String name,
        @Size(max = 500) String description
) {}
