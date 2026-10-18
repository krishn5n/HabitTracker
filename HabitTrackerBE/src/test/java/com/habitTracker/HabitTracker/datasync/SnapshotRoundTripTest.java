package com.habitTracker.HabitTracker.datasync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:snapshot-round-trip;DB_CLOSE_DELAY=-1",
        "app.data-sync.enabled=false",
        // Test-only key; the real one comes from DATA_ENCRYPTION_KEY in Render.
        "app.data-sync.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class SnapshotRoundTripTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SnapshotExporter exporter;

    @Autowired
    private SnapshotImporter importer;

    @Autowired
    private SnapshotCodec codec;

    @Test
    void exportsEncryptsAndRestoresEveryTable() throws Exception {
        seed();

        byte[] sealed = codec.seal(exporter.export());
        // Nothing readable should survive encryption.
        assertThat(new String(sealed)).doesNotContain("someone@example.com", "Morning run");

        wipe();
        importer.importSnapshot(codec.open(sealed));

        assertThat(count("app_user")).isEqualTo(1);
        assertThat(count("webauthn_credential")).isEqualTo(1);
        assertThat(count("habit")).isEqualTo(1);
        assertThat(count("habit_field")).isEqualTo(1);
        assertThat(count("entry")).isEqualTo(1);

        Map<String, Object> user = jdbcTemplate.queryForMap("SELECT * FROM app_user");
        assertThat(user.get("EMAIL")).isEqualTo("someone@example.com");
        assertThat(((java.sql.Date) user.get("DATE_OF_BIRTH")).toLocalDate()).isEqualTo(LocalDate.of(1995, 4, 17));
        assertThat(((Number) user.get("USER_SEQ_ID")).longValue()).isEqualTo(1L);

        Map<String, Object> entry = jdbcTemplate.queryForMap("SELECT * FROM entry");
        assertThat(entry.get("ENTRY_VALUE")).isEqualTo("5.2");
        assertThat(entry.get("COMPLETED")).isEqualTo(Boolean.TRUE);

        // The identity sequence has to clear the restored ids.
        jdbcTemplate.update("INSERT INTO habit (user_seq_id, name, active, created_at) VALUES (1, 'Second', TRUE, ?)",
                LocalDateTime.now());
        assertThat(jdbcTemplate.queryForObject("SELECT MAX(habit_seq_id) FROM habit", Long.class)).isEqualTo(2L);
    }

    @Test
    void sealedSnapshotCannotBeOpenedWithAnotherKey() throws Exception {
        byte[] sealed = codec.seal("{\"tables\":{}}".getBytes());

        DataSyncProperties other = new DataSyncProperties();
        other.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
        SnapshotCodec otherCodec = new SnapshotCodec(new tools.jackson.databind.ObjectMapper(), other);

        assertThat(catchThrowable(() -> otherCodec.open(sealed))).isNotNull();
    }

    private static Throwable catchThrowable(ThrowingCall call) {
        try {
            call.run();
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    private interface ThrowingCall {
        void run() throws Exception;
    }

    private void seed() {
        wipe();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        jdbcTemplate.update("INSERT INTO app_user (user_seq_id, display_name, email, gender, date_of_birth, "
                        + "weight_kg, height_cm, created_at, updated_at) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)",
                "Someone", "someone@example.com", "MALE", LocalDate.of(1995, 4, 17), 72.50, 178.00, now, now);
        jdbcTemplate.update("INSERT INTO webauthn_credential (credential_seq_id, user_seq_id, credential_id, "
                        + "public_key_cose, sign_count, aaguid, transports, created_at) VALUES (1, 1, ?, ?, 3, ?, ?, ?)",
                "cred-abc", "cose-key-bytes", "aaguid-1", "internal", now);
        jdbcTemplate.update("INSERT INTO habit (habit_seq_id, user_seq_id, name, description, active, created_at) "
                + "VALUES (1, 1, 'Morning run', 'Daily', TRUE, ?)", now);
        jdbcTemplate.update("INSERT INTO habit_field (habit_field_seq_id, habit_seq_id, field_name, field_type, "
                + "unit, goal_value, created_at) VALUES (1, 1, 'Distance', 'NUMBER', 'km', 5.00, ?)", now);
        jdbcTemplate.update("INSERT INTO entry (entry_seq_id, habit_field_seq_id, entry_value, entry_date, "
                        + "entry_timestamp, updated_at, updated_by, completed) VALUES (1, 1, '5.2', ?, ?, ?, 1, TRUE)",
                LocalDate.now(), now, now);
    }

    private void wipe() {
        jdbcTemplate.update("DELETE FROM entry");
        jdbcTemplate.update("DELETE FROM habit_field");
        jdbcTemplate.update("DELETE FROM habit");
        jdbcTemplate.update("DELETE FROM webauthn_credential");
        jdbcTemplate.update("DELETE FROM app_user");
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }
}
