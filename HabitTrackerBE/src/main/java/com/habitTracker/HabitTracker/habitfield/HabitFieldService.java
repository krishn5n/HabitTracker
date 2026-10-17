package com.habitTracker.HabitTracker.habitfield;

import com.habitTracker.HabitTracker.habit.Habit;
import com.habitTracker.HabitTracker.habit.HabitRepository;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldCreateRequest;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldResponse;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HabitFieldService {

    private final HabitFieldRepository habitFieldRepository;
    private final HabitRepository habitRepository;

    @Transactional
    public HabitFieldResponse createField(Long habitSeqId, HabitFieldCreateRequest request) {
        Habit habit = habitRepository.findById(habitSeqId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found: " + habitSeqId));

        validateGoal(request.fieldType(), request.goalValue());

        HabitField field = new HabitField();
        field.setHabit(habit);
        field.setFieldName(request.fieldName() != null ? request.fieldName() : habit.getName());
        field.setFieldType(request.fieldType());
        field.setUnit(request.unit());
        field.setGoalValue(request.goalValue());
        field.setCreatedAt(LocalDateTime.now());
        field = habitFieldRepository.save(field);

        return toResponse(field);
    }

    @Transactional(readOnly = true)
    public Page<HabitFieldResponse> listFields(Long habitSeqId, Pageable pageable) {
        return habitFieldRepository.findByHabit(habitSeqId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public HabitFieldResponse getField(Long habitFieldSeqId) {
        return toResponse(findOrThrow(habitFieldSeqId));
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
        if (request.goalValue() != null) {
            validateGoal(field.getFieldType(), request.goalValue());
            field.setGoalValue(request.goalValue());
        }

        return toResponse(field);
    }

    private void validateGoal(HabitFieldType fieldType, BigDecimal goalValue) {
        if (goalValue == null) {
            return;
        }
        if (fieldType != HabitFieldType.NUMBER && fieldType != HabitFieldType.DURATION) {
            throw new IllegalArgumentException("goalValue only applies to NUMBER or DURATION fields, not " + fieldType);
        }
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
                field.getUnit(),
                field.getGoalValue()
        );
    }
}
