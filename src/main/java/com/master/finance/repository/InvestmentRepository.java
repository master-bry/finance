package com.master.finance.repository;

import com.master.finance.model.Investment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvestmentRepository extends MongoRepository<Investment, String> {
    List<Investment> findByUserId(String userId);
    List<Investment> findByUserIdAndStatus(String userId, String status);
    List<Investment> findByUserIdAndType(String userId, String type);
}