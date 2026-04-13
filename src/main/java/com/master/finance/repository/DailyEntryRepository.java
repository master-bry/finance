package com.master.finance.repository;

import com.master.finance.model.DailyEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyEntryRepository extends MongoRepository<DailyEntry, String> {

    List<DailyEntry> findByUserIdAndDeletedFalseOrderByDateDesc(String userId);

    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<DailyEntry> findByUserIdAndDateBetween(String userId, LocalDateTime start, LocalDateTime end);

    default Optional<DailyEntry> findTodayEntry(String userId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<DailyEntry> entries = findByUserIdAndDateBetween(userId, startOfDay, endOfDay);
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0));
    }

    List<DailyEntry> findByUserIdAndDeletedFalse(String userId);

    // For recalculating balances from a specific date onward
    @Query("{ 'userId': ?0, 'date': { $gte: ?1 }, 'deleted': false }")
    List<DailyEntry> findByUserIdAndDateGreaterThanEqualOrderByDateAsc(String userId, LocalDateTime date);
}