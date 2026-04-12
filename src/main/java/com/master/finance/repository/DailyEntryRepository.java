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
    
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<DailyEntry> findByUserIdOrderByDateDesc(String userId);
    
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<DailyEntry> findByUserIdAndDateBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'userId': ?0, 'date': ?1, 'deleted': false }")
    Optional<DailyEntry> findByUserIdAndDate(String userId, LocalDateTime date);
    
    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'date': -1 }")
    Optional<DailyEntry> findLatestByUserId(String userId);
}