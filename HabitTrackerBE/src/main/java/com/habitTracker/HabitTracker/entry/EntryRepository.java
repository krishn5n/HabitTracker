package com.habitTracker.HabitTracker.entry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntryRepository extends JpaRepository<Entry, Long> {

    @Query("SELECT e FROM Entry e WHERE e.habitField.habitFieldSeqId = :habitFieldSeqId AND e.entryDate = :entryDate")
    Optional<Entry> findByHabitFieldAndDate(@Param("habitFieldSeqId") Long habitFieldSeqId, @Param("entryDate") LocalDate entryDate);

    @Query("SELECT e FROM Entry e WHERE e.habitField.habitFieldSeqId = :habitFieldSeqId "
            + "AND (:fromDate IS NULL OR e.entryDate >= :fromDate) "
            + "AND (:toDate IS NULL OR e.entryDate <= :toDate) "
            + "ORDER BY e.entryDate DESC")
    Page<Entry> findByHabitField(@Param("habitFieldSeqId") Long habitFieldSeqId,
                                  @Param("fromDate") LocalDate fromDate,
                                  @Param("toDate") LocalDate toDate,
                                  Pageable pageable);

    @Query("SELECT e FROM Entry e WHERE e.habitField.habitFieldSeqId = :habitFieldSeqId ORDER BY e.entryDate DESC")
    List<Entry> findLatestByHabitField(@Param("habitFieldSeqId") Long habitFieldSeqId, Pageable pageable);

    @Query("SELECT MIN(e.entryDate) FROM Entry e WHERE e.habitField.habitFieldSeqId = :habitFieldSeqId")
    Optional<LocalDate> findInceptionDate(@Param("habitFieldSeqId") Long habitFieldSeqId);

    @Query("SELECT e.entryDate FROM Entry e WHERE e.habitField.habitFieldSeqId = :habitFieldSeqId "
            + "AND e.entryDate >= :windowStart ORDER BY e.entryDate")
    List<LocalDate> findCompletedDates(@Param("habitFieldSeqId") Long habitFieldSeqId, @Param("windowStart") LocalDate windowStart);

    @Query("SELECT e FROM Entry e WHERE e.habitField.habitFieldSeqId IN :habitFieldSeqIds AND e.entryDate = :entryDate")
    List<Entry> findByHabitFieldsAndDate(@Param("habitFieldSeqIds") List<Long> habitFieldSeqIds, @Param("entryDate") LocalDate entryDate);
}
