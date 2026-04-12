package com.master.finance.repository;

import com.master.finance.model.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoalRepository extends MongoRepository<Goal, String> {
    
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Goal> findByUserIdAndDeletedFalse(String userId);
    
    @Query("{ 'userId': ?0, 'achieved': false, 'deleted': false }")
    List<Goal> findByUserIdAndAchievedFalseAndDeletedFalse(String userId);
}