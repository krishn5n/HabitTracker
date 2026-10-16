package com.habitTracker.HabitTracker.entry;

import com.habitTracker.HabitTracker.entry.dto.ConsistencyResponse;
import com.habitTracker.HabitTracker.entry.dto.EntryQueryParams;
import com.habitTracker.HabitTracker.entry.dto.EntryResponse;
import com.habitTracker.HabitTracker.entry.dto.EntryUpsertRequest;
import com.habitTracker.HabitTracker.habitfield.HabitField;
import com.habitTracker.HabitTracker.habitfield.HabitFieldRepository;
import com.habitTracker.HabitTracker.user.User;
import com.habitTracker.HabitTracker.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EntryService {

    private final EntryRepository entryRepository;
    private final HabitFieldRepository habitFieldRepository;
    private final UserRepository userRepository;

    @Transactional
    public EntryResponse upsertEntry(Long habitFieldSeqId, EntryUpsertRequest request, Long updatedByUserSeqId) {
        HabitField habitField = habitFieldRepository.findById(habitFieldSeqId)
                .orElseThrow(() -> new EntityNotFoundException("Habit field not found: " + habitFieldSeqId));
        User updatedBy = userRepository.findById(updatedByUserSeqId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + updatedByUserSeqId));

        LocalDate entryDate = request.entryDate() != null ? request.entryDate() : LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Entry entry = entryRepository.findByHabitFieldAndDate(habitFieldSeqId, entryDate)
                .orElseGet(() -> {
                    Entry newEntry = new Entry();
                    newEntry.setHabitField(habitField);
                    newEntry.setEntryDate(entryDate);
                    newEntry.setEntryTimestamp(now);
                    return newEntry;
                });

        entry.setEntryValue(request.entryValue());
        entry.setUpdatedAt(now);
        entry.setUpdatedBy(updatedBy);
        entry.setCompleted(isCompleted(habitField, request.entryValue()));
        entry = entryRepository.save(entry);

        return toResponse(entry);
    }

    private boolean isCompleted(HabitField habitField, String entryValue) {
        BigDecimal goalValue = habitField.getGoalValue();
        if (goalValue == null) {
            return true;
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(entryValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("entryValue must be numeric for a goal-based field: " + entryValue);
        }
        return parsed.compareTo(goalValue) >= 0;
    }

    @Transactional(readOnly = true)
    public Page<EntryResponse> listEntries(Long habitFieldSeqId, EntryQueryParams params, Pageable pageable) {
        return entryRepository.findByHabitField(habitFieldSeqId, params.fromDate(), params.toDate(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<EntryResponse> latestEntry(Long habitFieldSeqId) {
        List<Entry> latest = entryRepository.findLatestByHabitField(habitFieldSeqId, PageRequest.of(0, 1));
        return latest.stream().findFirst().map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConsistencyResponse consistency(Long habitFieldSeqId) {
        habitFieldRepository.findById(habitFieldSeqId)
                .orElseThrow(() -> new EntityNotFoundException("Habit field not found: " + habitFieldSeqId));

        LocalDate today = LocalDate.now();
        LocalDate inceptionDate = entryRepository.findInceptionDate(habitFieldSeqId).orElse(today);
        LocalDate windowStart = inceptionDate.isAfter(today.minusWeeks(4)) ? inceptionDate : today.minusWeeks(4);

        List<ConsistencyResponse.EntryStatus> entries = entryRepository.findInWindow(habitFieldSeqId, windowStart)
                .stream()
                .map(e -> new ConsistencyResponse.EntryStatus(e.getEntryDate(), e.isCompleted()))
                .toList();

        return new ConsistencyResponse(habitFieldSeqId, windowStart, today, entries);
    }

    private EntryResponse toResponse(Entry entry) {
        return new EntryResponse(
                entry.getEntrySeqId(),
                entry.getHabitField().getHabitFieldSeqId(),
                entry.getEntryValue(),
                entry.getEntryDate(),
                entry.getEntryTimestamp(),
                entry.getUpdatedAt(),
                entry.getUpdatedBy().getUserSeqId(),
                entry.isCompleted()
        );
    }
}
