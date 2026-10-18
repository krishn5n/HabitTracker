package com.habitTracker.HabitTracker.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Target for Render's health check. Deliberately behind the data-sync readiness
 * gate: while the snapshot is being restored this returns 503, so the instance
 * is only reported healthy once it can actually serve data.
 */
@RestController
public class HealthController {

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("status", "up");
    }
}
