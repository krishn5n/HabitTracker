package com.habitTracker.HabitTracker.entry;

import com.habitTracker.HabitTracker.entry.dto.ConsistencyResponse;
import com.habitTracker.HabitTracker.entry.dto.EntryQueryParams;
import com.habitTracker.HabitTracker.entry.dto.EntryResponse;
import com.habitTracker.HabitTracker.entry.dto.EntryUpsertRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/habit-fields/{habitFieldSeqId}")
@RequiredArgsConstructor
public class EntryController {

    private final EntryService entryService;

    @PutMapping("/entries")
    public EntryResponse upsertEntry(@PathVariable Long habitFieldSeqId,
                                      @Valid @RequestBody EntryUpsertRequest request,
                                      @AuthenticationPrincipal Long userSeqId) {
        return entryService.upsertEntry(habitFieldSeqId, request, userSeqId);
    }

    @GetMapping("/entries")
    public Page<EntryResponse> listEntries(@PathVariable Long habitFieldSeqId,
                                            @ParameterObject EntryQueryParams params,
                                            @ParameterObject Pageable pageable) {
        return entryService.listEntries(habitFieldSeqId, params, pageable);
    }

    @GetMapping("/entries/latest")
    public EntryResponse latestEntry(@PathVariable Long habitFieldSeqId) {
        return entryService.latestEntry(habitFieldSeqId)
                .orElseThrow(() -> new EntityNotFoundException("No entries found for habit field: " + habitFieldSeqId));
    }

    @GetMapping("/consistency")
    public ConsistencyResponse consistency(@PathVariable Long habitFieldSeqId) {
        return entryService.consistency(habitFieldSeqId);
    }
}
