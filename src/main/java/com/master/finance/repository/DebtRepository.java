package com.master.finance.repository;

import com.master.finance.model.Debt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DebtRepository extends MongoRepository<Debt, String> {
    List<Debt> findByUserId(String userId);
    List<Debt> findByUserIdAndType(String userId, String type);
    List<Debt> findByUserIdAndStatus(String userId, String status);
    List<Debt> findByUserIdAndTypeAndStatusNot(String userId, String type, String status);
}