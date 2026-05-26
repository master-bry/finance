package com.master.finance.repository;

import com.master.finance.model.RecurringTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends MongoRepository<RecurringTransaction, String> {
    List<RecurringTransaction> findByUserIdAndDeletedFalse(String userId);
    List<RecurringTransaction> findByActiveTrueAndDeletedFalseAndNextDateLessThanEqual(LocalDate date);
}
