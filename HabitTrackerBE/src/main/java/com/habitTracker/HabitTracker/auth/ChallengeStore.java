package com.habitTracker.HabitTracker.auth;

import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.util.Base64UrlUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral challenge cache keyed by email. Not a DB entity - a challenge only needs
 * to survive the few seconds between options and verify, and must never outlive a restart.
 * Discoverable (passkey autofill) logins have no email up front, so those challenges are
 * keyed by their own base64url value instead.
 */
@Component
public class ChallengeStore {

    private static final long TTL_MINUTES = 5;

    private final Map<String, Entry> challenges = new ConcurrentHashMap<>();
    private final Map<String, Entry> discoverableChallenges = new ConcurrentHashMap<>();

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

    public Challenge issueDiscoverable() {
        Challenge challenge = new DefaultChallenge();
        discoverableChallenges.put(Base64UrlUtil.encodeToString(challenge.getValue()),
                new Entry(challenge, Instant.now().plusSeconds(TTL_MINUTES * 60)));
        return challenge;
    }

    public Challenge consumeDiscoverable(String challenge) {
        Entry entry = discoverableChallenges.remove(challenge);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            throw new IllegalStateException("No valid discoverable challenge found");
        }
        return entry.challenge;
    }

    @Scheduled(fixedRate = 60_000)
    void sweepExpired() {
        Instant now = Instant.now();
        challenges.values().removeIf(entry -> entry.expiresAt.isBefore(now));
        discoverableChallenges.values().removeIf(entry -> entry.expiresAt.isBefore(now));
    }

    private record Entry(Challenge challenge, Instant expiresAt) {}
}
