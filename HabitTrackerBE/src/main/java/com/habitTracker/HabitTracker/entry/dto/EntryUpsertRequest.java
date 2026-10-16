package com.habitTracker.HabitTracker.entry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EntryUpsertRequest(
        @NotBlank @Size(max = 255) String entryValue,
        LocalDate entryDate
) {}
