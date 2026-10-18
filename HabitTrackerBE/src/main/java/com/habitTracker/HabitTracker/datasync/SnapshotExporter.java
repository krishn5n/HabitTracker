package com.habitTracker.HabitTracker.datasync;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;

/**
 * Reads every row of every snapshot table into a compact, self-describing JSON
 * document. Plain JDBC on purpose: JPA with IDENTITY ids cannot round-trip the
 * primary keys, and the snapshot has to survive entity refactors.
 */
@Component
public class SnapshotExporter {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SnapshotExporter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public byte[] export() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("version", 1);
        root.put("exportedAt", Instant.now().toString());

        ObjectNode tables = root.putObject("tables");
        for (SnapshotTables.Table table : SnapshotTables.ALL) {
            tables.set(table.name(), exportTable(table.name()));
        }
        return objectMapper.writeValueAsBytes(root);
    }

    private ObjectNode exportTable(String tableName) {
        return jdbcTemplate.query("SELECT * FROM " + tableName, rs -> {
            ObjectNode node = objectMapper.createObjectNode();
            ArrayNode columns = node.putArray("columns");
            ArrayNode rows = node.putArray("rows");

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnLabel(i).toLowerCase());
            }

            while (rs.next()) {
                ArrayNode row = rows.addArray();
                for (int i = 1; i <= columnCount; i++) {
                    addValue(row, rs, i);
                }
            }
            return node;
        });
    }

    /** Normalises SQL values to JSON scalars that {@link SnapshotImporter} can read back. */
    private void addValue(ArrayNode row, ResultSet rs, int index) throws java.sql.SQLException {
        Object value = rs.getObject(index);
        if (value == null) {
            row.addNull();
        } else if (value instanceof Timestamp timestamp) {
            row.add(timestamp.toLocalDateTime().toString());
        } else if (value instanceof Date date) {
            row.add(date.toLocalDate().toString());
        } else if (value instanceof BigDecimal decimal) {
            row.add(decimal.toPlainString());
        } else if (value instanceof Boolean flag) {
            row.add(flag);
        } else if (value instanceof Number number) {
            row.add(number.longValue());
        } else if (value instanceof byte[] bytes) {
            row.add(Base64.getEncoder().encodeToString(bytes));
        } else {
            row.add(value.toString());
        }
    }
}
