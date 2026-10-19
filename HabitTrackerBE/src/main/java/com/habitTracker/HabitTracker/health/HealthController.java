package com.habitTracker.HabitTracker.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Target for Render's health check. Deliberately behind the data-sync readiness
 * gate: while the snapshot is being restored this returns 503, so the instance
 * is only reported healthy once it can actually serve data.
 */
@Slf4j
@RestController
public class HealthController {

    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("status", "up");
    }

    /**
     * Keep-alive target for the frontend. No readiness gate, no body — just
     * proof the instance woke up, so it stays cheap to call on a timer.
     */
    @GetMapping("/ping")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ping() {
        log.info("Ping received, keeping service alive");
    }
}
