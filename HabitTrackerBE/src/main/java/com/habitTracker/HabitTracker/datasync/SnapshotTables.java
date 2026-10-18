package com.habitTracker.HabitTracker.datasync;

import java.util.List;

/**
 * The tables that make up a snapshot, parent-first so inserts satisfy the
 * foreign keys. Add new entities here when the schema grows.
 */
final class SnapshotTables {

    record Table(String name, String idColumn) {
    }

    static final List<Table> ALL = List.of(
            new Table("app_user", "user_seq_id"),
            new Table("webauthn_credential", "credential_seq_id"),
            new Table("habit", "habit_seq_id"),
            new Table("habit_field", "habit_field_seq_id"),
            new Table("entry", "entry_seq_id")
    );

    private SnapshotTables() {
    }
}
