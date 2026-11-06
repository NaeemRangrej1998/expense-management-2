package com.donation.service;

import com.donation.entity.AuditLog;
import com.donation.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String entityType, String entityId,
                    Object oldValues, Object newValues) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValues(oldValues != null ? objectMapper.writeValueAsString(oldValues) : null)
                .newValues(newValues != null ? objectMapper.writeValueAsString(newValues) : null)
                .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log: action={}, entity={}/{}", action, entityType, entityId, e);
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, String action, String entityType, String entityId) {
        log(userId, action, entityType, entityId, null, null);
    }
}
