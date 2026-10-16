package com.habitTracker.HabitTracker.entry.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EntryResponse(
        Long entrySeqId,
        Long habitFieldSeqId,
        String entryValue,
        LocalDate entryDate,
        LocalDateTime entryTimestamp,
        LocalDateTime updatedAt,
        Long updatedBy
) {}
