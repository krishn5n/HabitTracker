package com.habitTracker.HabitTracker.habit;

import com.habitTracker.HabitTracker.habit.dto.HabitCreateRequest;
import com.habitTracker.HabitTracker.habit.dto.HabitQueryParams;
import com.habitTracker.HabitTracker.habit.dto.HabitResponse;
import com.habitTracker.HabitTracker.habit.dto.HabitUpdateRequest;
import com.habitTracker.HabitTracker.user.User;
import com.habitTracker.HabitTracker.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    @Transactional
    public HabitResponse createHabit(HabitCreateRequest request) {
        User user = userRepository.findById(request.userSeqId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.userSeqId()));

        Habit habit = new Habit();
        habit.setUser(user);
        habit.setName(request.name());
        habit.setDescription(request.description());
        habit.setActive(true);
        habit.setCreatedAt(LocalDateTime.now());
        habit = habitRepository.save(habit);

        return toResponse(habit);
    }

    @Transactional(readOnly = true)
    public Page<HabitResponse> listHabits(HabitQueryParams params, Pageable pageable) {
        return habitRepository.findByUser(params.userSeqId(), params.activeOnly(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public HabitResponse getHabit(Long habitSeqId) {
        return toResponse(findOrThrow(habitSeqId));
    }

    @Transactional
    public HabitResponse updateHabit(Long habitSeqId, HabitUpdateRequest request) {
        Habit habit = findOrThrow(habitSeqId);

        if (request.name() != null) {
            habit.setName(request.name());
        }
        if (request.description() != null) {
            habit.setDescription(request.description());
        }

        return toResponse(habit);
    }

    @Transactional
    public void deactivateHabit(Long habitSeqId) {
        Habit habit = findOrThrow(habitSeqId);
        habit.setActive(false);
    }

    private Habit findOrThrow(Long habitSeqId) {
        return habitRepository.findById(habitSeqId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found: " + habitSeqId));
    }

    private HabitResponse toResponse(Habit habit) {
        return new HabitResponse(
                habit.getHabitSeqId(),
                habit.getUser().getUserSeqId(),
                habit.getName(),
                habit.getDescription(),
                habit.isActive()
        );
    }
}
