package com.habitTracker.HabitTracker.datasync;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class DataSyncConfig {

    /** Registered ahead of the security chain so the readiness gate answers first. */
    @Bean
    public FilterRegistrationBean<DataSyncFilter> dataSyncFilter(SyncState state) {
        FilterRegistrationBean<DataSyncFilter> registration = new FilterRegistrationBean<>(new DataSyncFilter(state));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Runs after the schema is created but while the readiness gate is still
     * closed, so no request can read a half-restored database.
     */
    @Bean
    public ApplicationRunner dataSyncRestoreRunner(DataSyncService dataSyncService, SyncState state) {
        return args -> {
            try {
                dataSyncService.restore();
            } finally {
                state.markReady();
            }
        };
    }
}
