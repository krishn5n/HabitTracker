package com.habitTracker.HabitTracker.habitfield;

import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldCreateRequest;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldResponse;
import com.habitTracker.HabitTracker.habitfield.dto.HabitFieldUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HabitFieldController {

    private final HabitFieldService habitFieldService;

    @PostMapping("/api/habits/{habitSeqId}/fields")
    public HabitFieldResponse createField(@PathVariable Long habitSeqId, @Valid @RequestBody HabitFieldCreateRequest request) {
        return habitFieldService.createField(habitSeqId, request);
    }

    @GetMapping("/api/habits/{habitSeqId}/fields")
    public Page<HabitFieldResponse> listFields(@PathVariable Long habitSeqId, @ParameterObject Pageable pageable) {
        return habitFieldService.listFields(habitSeqId, pageable);
    }

    @GetMapping("/api/habit-fields/{habitFieldSeqId}")
    public HabitFieldResponse getField(@PathVariable Long habitFieldSeqId) {
        return habitFieldService.getField(habitFieldSeqId);
    }

    @PutMapping("/api/habit-fields/{habitFieldSeqId}")
    public HabitFieldResponse updateField(@PathVariable Long habitFieldSeqId, @Valid @RequestBody HabitFieldUpdateRequest request) {
        return habitFieldService.updateField(habitFieldSeqId, request);
    }

    @DeleteMapping("/api/habit-fields/{habitFieldSeqId}")
    public void deleteField(@PathVariable Long habitFieldSeqId) {
        habitFieldService.deleteField(habitFieldSeqId);
    }
}
