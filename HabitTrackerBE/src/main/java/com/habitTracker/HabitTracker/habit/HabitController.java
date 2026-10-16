package com.habitTracker.HabitTracker.habit;

import com.habitTracker.HabitTracker.habit.dto.HabitCreateRequest;
import com.habitTracker.HabitTracker.habit.dto.HabitQueryParams;
import com.habitTracker.HabitTracker.habit.dto.HabitResponse;
import com.habitTracker.HabitTracker.habit.dto.HabitUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    @PostMapping
    public HabitResponse createHabit(@Valid @RequestBody HabitCreateRequest request) {
        return habitService.createHabit(request);
    }

    @GetMapping
    public List<HabitResponse> listHabits(@ParameterObject @Valid HabitQueryParams params) {
        return habitService.listHabits(params);
    }

    @GetMapping("/{habitSeqId}")
    public HabitResponse getHabit(@PathVariable Long habitSeqId) {
        return habitService.getHabit(habitSeqId);
    }

    @PutMapping("/{habitSeqId}")
    public HabitResponse updateHabit(@PathVariable Long habitSeqId, @Valid @RequestBody HabitUpdateRequest request) {
        return habitService.updateHabit(habitSeqId, request);
    }

    @DeleteMapping("/{habitSeqId}")
    public void deleteHabit(@PathVariable Long habitSeqId) {
        habitService.deactivateHabit(habitSeqId);
    }
}
