package com.habitTracker.HabitTracker.entry.dto;

import java.time.LocalDate;
import java.util.List;

public record ConsistencyResponse(
        Long habitFieldSeqId,
        LocalDate windowStart,
        LocalDate windowEnd,
        List<LocalDate> completedDates
) {}
