package com.master.finance.repository;

import com.master.finance.model.Budget;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface BudgetRepository extends MongoRepository<Budget, String> {
    Optional<Budget> findByUserIdAndMonth(String userId, String month);
    List<Budget> findByUserIdOrderByMonthDesc(String userId);
}