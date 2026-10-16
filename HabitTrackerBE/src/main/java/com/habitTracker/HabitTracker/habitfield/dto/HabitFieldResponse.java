package com.habitTracker.HabitTracker.habitfield.dto;

import com.habitTracker.HabitTracker.habitfield.HabitFieldType;

import java.math.BigDecimal;

public record HabitFieldResponse(
        Long habitFieldSeqId,
        Long habitSeqId,
        String fieldName,
        HabitFieldType fieldType,
        String unit,
        BigDecimal goalValue
) {}
