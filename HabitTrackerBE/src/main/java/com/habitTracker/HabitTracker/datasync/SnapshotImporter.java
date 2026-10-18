package com.habitTracker.HabitTracker.datasync;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replaces the local database contents with a snapshot, primary keys included,
 * then moves each identity sequence past the restored rows so the next insert
 * does not collide.
 */
@Component
public class SnapshotImporter {

    private static final Logger log = LoggerFactory.getLogger(SnapshotImporter.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SnapshotImporter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void importSnapshot(byte[] snapshotJson) throws Exception {
        JsonNode tables = objectMapper.readTree(snapshotJson).path("tables");

        List<SnapshotTables.Table> order = SnapshotTables.ALL;
        for (int i = order.size() - 1; i >= 0; i--) {
            jdbcTemplate.update("DELETE FROM " + order.get(i).name());
        }

        int total = 0;
        for (SnapshotTables.Table table : order) {
            total += importTable(table, tables.path(table.name()));
        }
        for (SnapshotTables.Table table : order) {
            restartIdentity(table);
        }
        log.info("Restored {} rows from snapshot", total);
    }

    private int importTable(SnapshotTables.Table table, JsonNode node) {
        if (node.isMissingNode() || !node.has("rows")) {
            log.info("Snapshot has no data for {}", table.name());
            return 0;
        }

        Map<String, Integer> liveTypes = liveColumnTypes(table.name());
        List<String> snapshotColumns = new ArrayList<>();
        node.path("columns").forEach(column -> snapshotColumns.add(column.asText().toLowerCase()));

        // Only columns that still exist are restored, so an older snapshot stays loadable.
        List<Integer> usedIndexes = new ArrayList<>();
        List<String> usedColumns = new ArrayList<>();
        for (int i = 0; i < snapshotColumns.size(); i++) {
            String column = snapshotColumns.get(i);
            if (liveTypes.containsKey(column)) {
                usedIndexes.add(i);
                usedColumns.add(column);
            } else {
                log.warn("Snapshot column {}.{} no longer exists; skipping it", table.name(), column);
            }
        }
        if (usedColumns.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO " + table.name() + " (" + String.join(", ", usedColumns) + ") VALUES ("
                + "?, ".repeat(usedColumns.size() - 1) + "?)";

        List<Object[]> batch = new ArrayList<>();
        for (JsonNode row : node.path("rows")) {
            Object[] values = new Object[usedColumns.size()];
            for (int i = 0; i < usedColumns.size(); i++) {
                values[i] = convert(row.path(usedIndexes.get(i)), liveTypes.get(usedColumns.get(i)));
            }
            batch.add(values);
        }

        jdbcTemplate.batchUpdate(sql, batch);
        return batch.size();
    }

    private Map<String, Integer> liveColumnTypes(String tableName) {
        return jdbcTemplate.query("SELECT * FROM " + tableName + " WHERE 1 = 0", rs -> {
            Map<String, Integer> types = new LinkedHashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                types.put(meta.getColumnLabel(i).toLowerCase(), meta.getColumnType(i));
            }
            return types;
        });
    }

    /** Turns a JSON scalar back into the Java type the column expects. */
    private Object convert(JsonNode value, int sqlType) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        String text = value.asText();
        return switch (sqlType) {
            case Types.BIGINT, Types.INTEGER, Types.SMALLINT, Types.TINYINT -> Long.valueOf(text);
            case Types.BOOLEAN, Types.BIT -> value.isBoolean() ? value.asBoolean() : Boolean.valueOf(text);
            case Types.DECIMAL, Types.NUMERIC, Types.DOUBLE, Types.FLOAT, Types.REAL -> new BigDecimal(text);
            case Types.DATE -> LocalDate.parse(text);
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> LocalDateTime.parse(text);
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> Base64.getDecoder().decode(text);
            default -> text;
        };
    }

    private void restartIdentity(SnapshotTables.Table table) {
        try {
            Long next = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(" + table.idColumn() + "), 0) + 1 FROM " + table.name(), Long.class);
            jdbcTemplate.execute("ALTER TABLE " + table.name() + " ALTER COLUMN " + table.idColumn()
                    + " RESTART WITH " + next);
        } catch (Exception e) {
            log.error("Could not restart the identity sequence for {}.{}; the next insert may collide",
                    table.name(), table.idColumn(), e);
        }
    }
}
