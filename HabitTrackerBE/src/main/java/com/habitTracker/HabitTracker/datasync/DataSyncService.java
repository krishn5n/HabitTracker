package com.habitTracker.HabitTracker.datasync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Periodically commits and pushes the H2 data file to GitHub so it survives
 * restarts/redeploys on hosts without durable local storage.
 */
@Service
public class DataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.data-sync.enabled:false}")
    private boolean enabled;

    @Value("${app.data-sync.repo-dir:.}")
    private String repoDir;

    @Value("${app.data-sync.data-path:data}")
    private String dataPath;

    @Value("${app.data-sync.branch:master}")
    private String branch;

    @Value("${app.data-sync.github-repo:}")
    private String githubRepo;

    public DataSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRateString = "${app.data-sync.push-interval-ms:840000}")
    public void pushSnapshot() {
        if (!enabled) {
            return;
        }
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            log.warn("GITHUB_TOKEN not set; skipping data sync push");
            return;
        }

        try {
            jdbcTemplate.execute("CHECKPOINT");

            run("add", dataPath);
            if (run("diff", "--cached", "--quiet", "--", dataPath) == 0) {
                return;
            }

            run("-c", "user.email=habit-sync@local", "-c", "user.name=Habit Sync Bot",
                    "commit", "-m", "chore: sync habit data snapshot");

            String pushUrl = "https://x-access-token:" + token + "@github.com/" + githubRepo + ".git";
            int pushExit = run("push", pushUrl, "HEAD:" + branch);
            if (pushExit != 0) {
                throw new IllegalStateException("git push failed with exit " + pushExit);
            }
            log.info("Pushed habit data snapshot to {}", branch);
        } catch (Exception e) {
            log.error("Data sync push failed", e);
        }
    }

    private int run(String... args) throws Exception {
        String[] command = new String[args.length + 1];
        command[0] = "git";
        System.arraycopy(args, 0, command, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(repoDir));
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("git command timed out");
        }
        String scrubbed = output.replace(System.getenv().getOrDefault("GITHUB_TOKEN", "\u0000\u0000\u0000"), "***");
        if (process.exitValue() != 0) {
            log.warn("git command output: {}", scrubbed);
        } else {
            log.debug("git command output: {}", scrubbed);
        }
        return process.exitValue();
    }
}
