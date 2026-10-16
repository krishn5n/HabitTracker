package com.habitTracker.HabitTracker.entry;

import com.habitTracker.HabitTracker.habitfield.HabitField;
import com.habitTracker.HabitTracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entry", uniqueConstraints = @UniqueConstraint(columnNames = {"habit_field_seq_id", "entry_date"}))
@Getter
@Setter
@NoArgsConstructor
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_seq_id")
    private Long entrySeqId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_field_seq_id", nullable = false)
    private HabitField habitField;

    @Column(name = "entry_value", length = 255, nullable = false)
    private String entryValue;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_timestamp", nullable = false)
    private LocalDateTime entryTimestamp;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    @Column(name = "completed", nullable = false, columnDefinition = "boolean default false")
    private boolean completed;
}
