package com.habitTracker.HabitTracker.habit.dto;

public record HabitResponse(
        Long habitSeqId,
        Long userSeqId,
        String name,
        String description,
        boolean active
) {}
