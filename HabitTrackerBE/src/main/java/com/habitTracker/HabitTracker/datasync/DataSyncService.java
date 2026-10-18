package com.habitTracker.HabitTracker.datasync;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Durable storage for a host with no durable disk: the working database lives
 * on the ephemeral filesystem, while the encrypted snapshot in a private GitHub
 * repository is the copy that actually survives a restart.
 */
@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final DataSyncProperties properties;
    private final GitHubContentsClient github;
    private final SnapshotCodec codec;
    private final SnapshotExporter exporter;
    private final SnapshotImporter importer;
    private final SyncState state;

    /** Pushes never overlap, so a hard and an idle push landing together serialise. */
    private final ReentrantLock pushLock = new ReentrantLock();

    public DataSyncService(DataSyncProperties properties,
                           GitHubContentsClient github,
                           SnapshotCodec codec,
                           SnapshotExporter exporter,
                           SnapshotImporter importer,
                           SyncState state) {
        this.properties = properties;
        this.github = github;
        this.codec = codec;
        this.exporter = exporter;
        this.importer = importer;
        this.state = state;
    }

    /** Pulls the snapshot and loads it into the database. Called once at startup. */
    public void restore() {
        if (!usable("restore")) {
            return;
        }
        try {
            Optional<byte[]> envelope = github.fetch();
            if (envelope.isEmpty()) {
                log.warn("No snapshot at {}/{} yet; starting with an empty database",
                        properties.getRepo(), properties.getPath());
                return;
            }
            importer.importSnapshot(codec.open(envelope.get()));
            state.clearDirty();
            log.info("Restored data from {}/{}", properties.getRepo(), properties.getPath());
        } catch (Exception e) {
            // Serving stale-but-present local data beats refusing to boot.
            log.error("Restore from GitHub failed; continuing with whatever is on local disk", e);
        }
    }

    /**
     * Exports, encrypts and commits the snapshot.
     *
     * @param reason what triggered this push, recorded in the commit message
     * @param onlyIfDirty skip when nothing has been written since the last push
     */
    public void push(String reason, boolean onlyIfDirty) {
        if (!usable("push")) {
            return;
        }
        if (onlyIfDirty && !state.isDirty()) {
            log.debug("Skipping {} push: no writes since the last snapshot", reason);
            return;
        }

        pushLock.lock();
        try {
            byte[] snapshot = codec.seal(exporter.export());
            github.push(snapshot, "chore: habit data snapshot (" + reason + ") " + Instant.now());
            state.clearDirty();
            log.info("Pushed snapshot ({}), {} bytes encrypted", reason, snapshot.length);
        } catch (Exception e) {
            // Leave the dirty flag set so the next tick retries.
            log.error("Snapshot push ({}) failed", reason, e);
        } finally {
            pushLock.unlock();
        }
    }

    @PreDestroy
    public void pushOnShutdown() {
        if (properties.isPushOnShutdown()) {
            // Best effort: Render allows a short grace period after SIGTERM.
            push("shutdown", true);
        }
    }

    private boolean usable(String action) {
        if (!properties.isEnabled()) {
            return false;
        }
        if (!github.isConfigured()) {
            log.warn("DATA_REPO/DATA_REPO_TOKEN not set; skipping data sync {}", action);
            return false;
        }
        if (!codec.hasKey()) {
            log.warn("DATA_ENCRYPTION_KEY not set; skipping data sync {}", action);
            return false;
        }
        return true;
    }
}
