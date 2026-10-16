package com.habitTracker.HabitTracker.habitfield;

import com.habitTracker.HabitTracker.habit.Habit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "habit_field")
@Getter
@Setter
@NoArgsConstructor
public class HabitField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "habit_field_seq_id")
    private Long habitFieldSeqId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_seq_id", nullable = false)
    private Habit habit;

    @Column(name = "field_name", length = 100, nullable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", length = 20, nullable = false)
    private HabitFieldType fieldType;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "goal_value", precision = 12, scale = 2)
    private BigDecimal goalValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
