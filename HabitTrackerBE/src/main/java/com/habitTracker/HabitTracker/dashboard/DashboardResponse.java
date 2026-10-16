package com.habitTracker.HabitTracker.dashboard;

import com.habitTracker.HabitTracker.habitfield.HabitFieldType;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(List<HabitDashboardItem> habits) {

    public record HabitDashboardItem(
            Long habitSeqId, String name,
            List<HabitFieldDashboardItem> fields
    ) {}

    public record HabitFieldDashboardItem(
            Long habitFieldSeqId, String fieldName, HabitFieldType fieldType, String unit,
            boolean completedToday, String todayValue, LocalDateTime updatedAt
    ) {}
}
