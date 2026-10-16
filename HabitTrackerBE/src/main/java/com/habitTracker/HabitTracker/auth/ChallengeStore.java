package com.habitTracker.HabitTracker.auth;

import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral challenge cache keyed by email. Not a DB entity - a challenge only needs
 * to survive the few seconds between options and verify, and must never outlive a restart.
 */
@Component
public class ChallengeStore {

    private static final long TTL_MINUTES = 5;

    private final Map<String, Entry> challenges = new ConcurrentHashMap<>();

    public Challenge issue(String email) {
        Challenge challenge = new DefaultChallenge();
        challenges.put(email, new Entry(challenge, Instant.now().plusSeconds(TTL_MINUTES * 60)));
        return challenge;
    }

    public Challenge consume(String email) {
        Entry entry = challenges.remove(email);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            throw new IllegalStateException("No valid challenge found for: " + email);
        }
        return entry.challenge;
    }

    @Scheduled(fixedRate = 60_000)
    void sweepExpired() {
        Instant now = Instant.now();
        challenges.values().removeIf(entry -> entry.expiresAt.isBefore(now));
    }

    private record Entry(Challenge challenge, Instant expiresAt) {}
}
