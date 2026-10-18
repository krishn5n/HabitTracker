package com.habitTracker.HabitTracker.datasync;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Settings for the GitHub-backed snapshot store. Everything that is a secret
 * (token, encryption key) comes from Render environment variables.
 */
@Component
@ConfigurationProperties(prefix = "app.data-sync")
@Getter
@Setter
public class DataSyncProperties {

    /** Master switch. Off locally, on in Render. */
    private boolean enabled = false;

    /** Private data repository as {@code owner/name}. Never the code repository. */
    private String repo = "";

    private String branch = "main";

    /** Path of the encrypted snapshot inside the data repository. */
    private String path = "snapshots/habittracker.json.enc";

    /** Fine-grained PAT with Contents read+write on the data repository only. */
    private String token = "";

    /** Base64-encoded 32-byte AES key. Lose it and the snapshots are unreadable. */
    private String encryptionKey = "";

    /** Idle timer: reset to this on every inbound request, pushes when it expires. */
    private int idleMinutes = 12;

    /** Hard timer: never reset by traffic, pushes when it expires. */
    private int hardMinutes = 30;

    /** How often the deadlines are checked. */
    private int tickSeconds = 15;

    /** Push one last snapshot when the container receives SIGTERM. */
    private boolean pushOnShutdown = true;
}
