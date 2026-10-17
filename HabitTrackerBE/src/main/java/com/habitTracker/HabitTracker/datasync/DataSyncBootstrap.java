package com.habitTracker.HabitTracker.datasync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Runs before the Spring context (and the H2 DataSource) is created, so the
 * on-disk data file reflects the latest GitHub snapshot before anything opens it.
 */
public class DataSyncBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DataSyncBootstrap.class);

    public static void pullLatest() {
        boolean enabled = Boolean.parseBoolean(System.getenv().getOrDefault("DATA_SYNC_ENABLED", "false"));
        if (!enabled) {
            return;
        }

        String repoDir = System.getenv().getOrDefault("DATA_SYNC_REPO_DIR", ".");
        String dataPath = System.getenv().getOrDefault("DATA_SYNC_DATA_PATH", "data");
        String branch = System.getenv().getOrDefault("DATA_SYNC_BRANCH", "master");

        try {
            run(repoDir, "git", "fetch", "origin", branch);
            int exit = run(repoDir, "git", "checkout", "origin/" + branch, "--", dataPath);
            if (exit == 0) {
                log.info("Restored {} from origin/{}", dataPath, branch);
            } else {
                log.warn("No remote snapshot of {} on origin/{}; starting with local/empty data", dataPath, branch);
            }
        } catch (Exception e) {
            log.warn("Could not fetch data snapshot from GitHub, continuing with local data file", e);
        }
    }

    private static int run(String dir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(dir));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.getInputStream().transferTo(System.out);
        boolean finished = process.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("git command timed out: " + String.join(" ", command));
        }
        return process.exitValue();
    }
}
