package com.habitTracker.HabitTracker.datasync;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Sits in front of everything else: holds requests off until the restore has
 * finished, resets the idle push timer on every request, and flags the data as
 * dirty whenever a request could have written something.
 */
public class DataSyncFilter extends OncePerRequestFilter {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final SyncState state;

    public DataSyncFilter(SyncState state) {
        this.state = state;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!state.isReady()) {
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.RETRY_AFTER, "5");
            response.getWriter().write("{\"error\":\"warming up\",\"detail\":\"restoring data snapshot\"}");
            return;
        }

        // Any traffic also resets Render's spin-down clock, so the idle push can wait.
        state.touch();
        if (!READ_METHODS.contains(request.getMethod())) {
            state.markDirty();
        }

        filterChain.doFilter(request, response);
    }
}
