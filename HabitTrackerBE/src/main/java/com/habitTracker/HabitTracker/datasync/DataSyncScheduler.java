package com.habitTracker.HabitTracker.datasync;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Single ticker for both deadlines. A tick is cheap, so the resolution loss
 * (up to one tick late) is irrelevant against a 12-minute window.
 */
@Component
public class DataSyncScheduler {

    private final DataSyncProperties properties;
    private final DataSyncService dataSyncService;
    private final SyncState state;

    public DataSyncScheduler(DataSyncProperties properties, DataSyncService dataSyncService, SyncState state) {
        this.properties = properties;
        this.dataSyncService = dataSyncService;
        this.state = state;
    }

    @Scheduled(fixedDelayString = "${app.data-sync.tick-seconds:15}", timeUnit = TimeUnit.SECONDS)
    public void tick() {
        // Pushing before the restore lands would overwrite the snapshot with an empty database.
        if (!properties.isEnabled() || !state.isReady()) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean idle = state.idleExpired(now);
        boolean hard = state.hardExpired(now);
        if (!idle && !hard) {
            return;
        }

        if (idle) {
            state.resetIdle(now);
        }
        if (hard) {
            state.resetHard(now);
        }

        String reason = idle && hard ? "idle+hard timer" : idle ? "idle timer" : "hard timer";
        dataSyncService.push(reason, true);
    }
}
