package com.master.finance.service;

import com.master.finance.model.AuditLog;
import com.master.finance.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(String userId, String action, String entityType, String entityId, String description) {
        AuditLog log = new AuditLog(userId, action, entityType, entityId, description);
        auditLogRepository.save(log);
    }

    public void log(String userId, String action, String entityType, String entityId, String description, String severity) {
        AuditLog log = new AuditLog(userId, action, entityType, entityId, description);
        log.setSeverity(severity);
        auditLogRepository.save(log);
    }

    @Async
    public void logAsync(String userId, String action, String entityType, String entityId, String description) {
        log(userId, action, entityType, entityId, description);
    }

    public List<AuditLog> getUserAuditLogs(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<AuditLog> getUserAuditLogsBetween(String userId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(userId, start, end);
    }

    public void cleanupOldLogs(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        auditLogRepository.deleteByTimestampBefore(cutoff);
    }
}
