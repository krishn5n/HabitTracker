package com.habitTracker.HabitTracker.entry.dto;

import java.time.LocalDate;

public record EntryQueryParams(
        LocalDate fromDate,
        LocalDate toDate
) {}
