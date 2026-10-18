package com.habitTracker.HabitTracker.datasync;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The two push deadlines, plus whether there is anything worth pushing and
 * whether the restore has finished.
 *
 * <p>The idle deadline is pushed forward by every inbound request: traffic
 * resets Render's 15-minute spin-down clock too, so the snapshot only needs to
 * be written once the app has gone quiet. The hard deadline ignores traffic so
 * a busy day still produces regular backups.
 */
@Component
public class SyncState {

    private final DataSyncProperties properties;

    private final AtomicLong idleDeadline = new AtomicLong();
    private final AtomicLong hardDeadline = new AtomicLong();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public SyncState(DataSyncProperties properties) {
        this.properties = properties;
        long now = System.currentTimeMillis();
        idleDeadline.set(now + idleWindow());
        hardDeadline.set(now + hardWindow());
        ready.set(!properties.isEnabled());
    }

    public boolean isReady() {
        return ready.get();
    }

    public void markReady() {
        ready.set(true);
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public void markDirty() {
        dirty.set(true);
    }

    public void clearDirty() {
        dirty.set(false);
    }

    public void touch() {
        idleDeadline.set(System.currentTimeMillis() + idleWindow());
    }

    public boolean idleExpired(long now) {
        return now >= idleDeadline.get();
    }

    public boolean hardExpired(long now) {
        return now >= hardDeadline.get();
    }

    public void resetIdle(long now) {
        idleDeadline.set(now + idleWindow());
    }

    public void resetHard(long now) {
        hardDeadline.set(now + hardWindow());
    }

    private long idleWindow() {
        return properties.getIdleMinutes() * 60_000L;
    }

    private long hardWindow() {
        return properties.getHardMinutes() * 60_000L;
    }
}
