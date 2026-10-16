package com.habitTracker.HabitTracker.habitfield;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitFieldRepository extends JpaRepository<HabitField, Long> {

    @Query("SELECT f FROM HabitField f WHERE f.habit.habitSeqId = :habitSeqId")
    Page<HabitField> findByHabit(@Param("habitSeqId") Long habitSeqId, Pageable pageable);
}
