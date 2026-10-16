package com.habitTracker.HabitTracker.habit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    @Query("SELECT h FROM Habit h WHERE h.user.userSeqId = :userSeqId AND (:activeOnly = false OR h.active = true)")
    List<Habit> findByUser(@Param("userSeqId") Long userSeqId, @Param("activeOnly") boolean activeOnly);
}
