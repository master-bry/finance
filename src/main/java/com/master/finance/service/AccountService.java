package com.master.finance.service;

import com.master.finance.model.Account;
import com.master.finance.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Account createAccount(Account account) {
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    public Optional<Account> getAccount(String id) {
        return accountRepository.findById(id);
    }

    public List<Account> getUserAccounts(String userId) {
        return accountRepository.findByUserIdAndDeletedFalse(userId);
    }

    public Account updateAccount(String id, Account updated) {
        return accountRepository.findById(id).map(account -> {
            account.setName(updated.getName());
            account.setType(updated.getType());
            account.setBankName(updated.getBankName());
            account.setAccountNumber(updated.getAccountNumber());
            account.setCurrency(updated.getCurrency());
            account.setNotes(updated.getNotes());
            account.setUpdatedAt(LocalDateTime.now());
            return accountRepository.save(account);
        }).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account updateBalance(String id, Double newBalance) {
        return accountRepository.findById(id).map(account -> {
            account.setBalance(newBalance);
            account.setUpdatedAt(LocalDateTime.now());
            return accountRepository.save(account);
        }).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public void deleteAccount(String id) {
        accountRepository.findById(id).ifPresent(account -> {
            account.setDeleted(true);
            accountRepository.save(account);
        });
    }

    public double getTotalBalance(String userId) {
        return accountRepository.findByUserIdAndDeletedFalse(userId).stream()
                .mapToDouble(a -> a.getBalance() != null ? a.getBalance() : 0)
                .sum();
    }
}
