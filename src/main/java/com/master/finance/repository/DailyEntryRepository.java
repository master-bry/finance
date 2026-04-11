package com.master.finance.repository;

import com.master.finance.model.DailyEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyEntryRepository extends MongoRepository<DailyEntry, String> {
    List<DailyEntry> findByUserIdOrderByDateDesc(String userId);
    List<DailyEntry> findByUserIdAndDateBetween(String userId, LocalDateTime start, LocalDateTime end);
    Optional<DailyEntry> findByUserIdAndDate(String userId, LocalDateTime date);
}