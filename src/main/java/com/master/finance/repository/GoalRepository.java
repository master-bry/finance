package com.master.finance.repository;

import com.master.finance.model.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoalRepository extends MongoRepository<Goal, String> {
    List<Goal> findByUserId(String userId);
    List<Goal> findByUserIdAndAchieved(String userId, boolean achieved);
    List<Goal> findByUserIdAndPriority(String userId, String priority);
}