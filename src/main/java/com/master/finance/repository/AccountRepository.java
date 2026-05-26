package com.master.finance.repository;

import com.master.finance.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {
    List<Account> findByUserIdAndDeletedFalse(String userId);
    List<Account> findByUserIdAndDeletedFalseAndType(String userId, String type);
}
