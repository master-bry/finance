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
    
    // Find by userId and date range (excluding soft deleted)
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<DailyEntry> findByUserIdAndDateBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    // Find by userId and specific date
    @Query("{ 'userId': ?0, 'date': ?1, 'deleted': false }")
    Optional<DailyEntry> findByUserIdAndDate(String userId, LocalDateTime date);
    
    // Find all entries for a user (excluding soft deleted)
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<DailyEntry> findByUserIdOrderByDateDesc(String userId);
    
    // Find entries by month and year
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<DailyEntry> findMonthlyEntries(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Count completed entries for a user
    @Query(value = "{ 'userId': ?0, 'completed': true, 'deleted': false }", count = true)
    long countCompletedEntriesByUserId(String userId);
    
    // Find entries by date range with pagination
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<DailyEntry> findEntriesByDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Find latest entry for a user
    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'date': -1 }")
    Optional<DailyEntry> findTopByUserIdOrderByDateDesc(String userId);
    
    // Find entries by mood
    @Query("{ 'userId': ?0, 'mood': ?1, 'deleted': false }")
    List<DailyEntry> findByUserIdAndMood(String userId, String mood);
    
    // Find entries with notes
    @Query("{ 'userId': ?0, 'notes': { $exists: true, $ne: [] }, 'deleted': false }")
    List<DailyEntry> findEntriesWithNotes(String userId);
}