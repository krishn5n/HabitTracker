package com.habitTracker.HabitTracker.habit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    @Query("SELECT h FROM Habit h WHERE h.user.userSeqId = :userSeqId AND (:activeOnly = false OR h.active = true)")
    Page<Habit> findByUser(@Param("userSeqId") Long userSeqId, @Param("activeOnly") boolean activeOnly, Pageable pageable);

    @Query("SELECT DISTINCT h FROM Habit h LEFT JOIN FETCH h.fields WHERE h.user.userSeqId = :userSeqId AND h.active = true")
    List<Habit> findActiveWithFields(@Param("userSeqId") Long userSeqId);
}
