package com.habitTracker.HabitTracker.habitfield;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HabitFieldRepository extends JpaRepository<HabitField, Long> {

    @Query("SELECT f FROM HabitField f WHERE f.habit.habitSeqId = :habitSeqId")
    List<HabitField> findByHabit(@Param("habitSeqId") Long habitSeqId);
}
