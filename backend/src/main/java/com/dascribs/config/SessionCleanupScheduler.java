package com.dascribs.config;

import com.dascribs.service.auth.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupScheduler {

    @Autowired
    private SessionService sessionService;

    @Scheduled(cron = "${app.security.session.cleanup-cron:0 0 2 * * ?}")
    public void cleanupExpiredSessions() {
        sessionService.cleanupExpiredSessions();
    }
}