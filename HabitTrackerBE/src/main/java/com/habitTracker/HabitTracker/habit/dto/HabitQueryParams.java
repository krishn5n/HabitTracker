package com.habitTracker.HabitTracker.habit.dto;

import jakarta.validation.constraints.NotNull;

public record HabitQueryParams(
        @NotNull Long userSeqId,
        boolean activeOnly
) {}
