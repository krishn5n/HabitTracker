package com.habitTracker.HabitTracker.datasync;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Reads and writes the single snapshot file through the GitHub Contents API.
 * Deliberately not git: the Render container has no clone and no push creds.
 */
@Component
public class GitHubContentsClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubContentsClient.class);
    private static final String API = "https://api.github.com";

    private final DataSyncProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Blob sha of the snapshot as we last saw it; the Contents API needs it to update. */
    private volatile String knownSha;

    public GitHubContentsClient(DataSyncProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return !properties.getRepo().isBlank() && !properties.getToken().isBlank();
    }

    /** Snapshot bytes from the data repository, or empty when the file does not exist yet. */
    public Optional<byte[]> fetch() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(contentsUrl() + "?ref=" + encode(properties.getBranch())))
                .GET());

        if (response.statusCode() == 404) {
            knownSha = null;
            return Optional.empty();
        }
        requireSuccess(response, "fetch snapshot");

        JsonNode body = objectMapper.readTree(response.body());
        knownSha = body.path("sha").asText(null);

        String content = body.path("content").asText("");
        if (!content.isBlank()) {
            return Optional.of(Base64.getMimeDecoder().decode(content));
        }
        // Files over 1 MB come back with an empty content field; read the blob instead.
        return Optional.of(fetchBlob(knownSha));
    }

    /** Commits the snapshot. Retries once with a refreshed sha if the stored one is stale. */
    public void push(byte[] snapshot, String commitMessage) throws Exception {
        HttpResponse<String> response = put(snapshot, commitMessage, knownSha);

        if (response.statusCode() == 409 || response.statusCode() == 422) {
            log.warn("Snapshot sha was stale ({}), refreshing and retrying once", response.statusCode());
            fetch();
            response = put(snapshot, commitMessage, knownSha);
        }
        requireSuccess(response, "push snapshot");

        knownSha = objectMapper.readTree(response.body()).path("content").path("sha").asText(null);
    }

    private HttpResponse<String> put(byte[] snapshot, String commitMessage, String sha) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("message", commitMessage);
        payload.put("content", Base64.getEncoder().encodeToString(snapshot));
        payload.put("branch", properties.getBranch());
        if (sha != null && !sha.isBlank()) {
            payload.put("sha", sha);
        }

        return send(HttpRequest.newBuilder()
                .uri(URI.create(contentsUrl()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(payload))));
    }

    private byte[] fetchBlob(String sha) throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(API + "/repos/" + properties.getRepo() + "/git/blobs/" + sha))
                .header("Accept", "application/vnd.github.raw")
                .GET());
        requireSuccess(response, "fetch snapshot blob");
        return response.body().getBytes(StandardCharsets.UTF_8);
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        HttpRequest request = builder
                .header("Authorization", "Bearer " + properties.getToken())
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "habit-tracker-data-sync")
                .timeout(Duration.ofSeconds(60))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void requireSuccess(HttpResponse<String> response, String what) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            // Body can echo request data but never the token, which only travels in headers.
            throw new IllegalStateException("GitHub " + what + " failed with HTTP " + status + ": "
                    + abbreviate(response.body()));
        }
    }

    private String contentsUrl() {
        return API + "/repos/" + properties.getRepo() + "/contents/" + properties.getPath();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 300 ? body : body.substring(0, 300) + "…";
    }
}
