package com.habitTracker.HabitTracker.habitfield;

import com.habitTracker.HabitTracker.habit.Habit;
import com.habitTracker.HabitTracker.habit.HabitRepository;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldCreateRequest;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldResponse;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitFieldService {

    private final HabitFieldRepository habitFieldRepository;
    private final HabitRepository habitRepository;

    @Transactional
    public HabitFieldResponse createField(Long habitSeqId, HabitFieldCreateRequest request) {
        Habit habit = habitRepository.findById(habitSeqId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found: " + habitSeqId));

        HabitField field = new HabitField();
        field.setHabit(habit);
        field.setFieldName(request.fieldName() != null ? request.fieldName() : habit.getName());
        field.setFieldType(request.fieldType());
        field.setUnit(request.unit());
        field.setCreatedAt(LocalDateTime.now());
        field = habitFieldRepository.save(field);

        return toResponse(field);
    }

    @Transactional(readOnly = true)
    public List<HabitFieldResponse> listFields(Long habitSeqId) {
        return habitFieldRepository.findByHabit(habitSeqId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HabitFieldResponse updateField(Long habitFieldSeqId, HabitFieldUpdateRequest request) {
        HabitField field = findOrThrow(habitFieldSeqId);

        if (request.fieldName() != null) {
            field.setFieldName(request.fieldName());
        }
        if (request.unit() != null) {
            field.setUnit(request.unit());
        }

        return toResponse(field);
    }

    @Transactional
    public void deleteField(Long habitFieldSeqId) {
        habitFieldRepository.delete(findOrThrow(habitFieldSeqId));
    }

    private HabitField findOrThrow(Long habitFieldSeqId) {
        return habitFieldRepository.findById(habitFieldSeqId)
                .orElseThrow(() -> new EntityNotFoundException("Habit field not found: " + habitFieldSeqId));
    }

    private HabitFieldResponse toResponse(HabitField field) {
        return new HabitFieldResponse(
                field.getHabitFieldSeqId(),
                field.getHabit().getHabitSeqId(),
                field.getFieldName(),
                field.getFieldType(),
                field.getUnit()
        );
    }
}
