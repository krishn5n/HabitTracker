package com.habitTracker.HabitTracker.dashboard;

import com.habitTracker.HabitTracker.entry.Entry;
import com.habitTracker.HabitTracker.entry.EntryRepository;
import com.habitTracker.HabitTracker.habit.Habit;
import com.habitTracker.HabitTracker.habit.HabitRepository;
import com.habitTracker.HabitTracker.habitfield.HabitField;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final HabitRepository habitRepository;
    private final EntryRepository entryRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long userSeqId) {
        List<Habit> habits = habitRepository.findActiveWithFields(userSeqId);

        List<Long> fieldIds = habits.stream()
                .flatMap(h -> h.getFields().stream())
                .map(HabitField::getHabitFieldSeqId)
                .toList();

        Map<Long, Entry> todayEntryByFieldId = fieldIds.isEmpty()
                ? Map.of()
                : entryRepository.findByHabitFieldsAndDate(fieldIds, LocalDate.now()).stream()
                        .collect(Collectors.toMap(e -> e.getHabitField().getHabitFieldSeqId(), Function.identity()));

        List<DashboardResponse.HabitDashboardItem> items = habits.stream()
                .map(habit -> new DashboardResponse.HabitDashboardItem(
                        habit.getHabitSeqId(),
                        habit.getName(),
                        habit.getFields().stream()
                                .map(field -> toFieldItem(field, todayEntryByFieldId.get(field.getHabitFieldSeqId())))
                                .toList()
                ))
                .toList();

        return new DashboardResponse(items);
    }

    private DashboardResponse.HabitFieldDashboardItem toFieldItem(HabitField field, Entry todayEntry) {
        return new DashboardResponse.HabitFieldDashboardItem(
                field.getHabitFieldSeqId(),
                field.getFieldName(),
                field.getFieldType(),
                field.getUnit(),
                todayEntry != null,
                todayEntry != null ? todayEntry.getEntryValue() : null,
                todayEntry != null ? todayEntry.getUpdatedAt() : null
        );
    }
}
