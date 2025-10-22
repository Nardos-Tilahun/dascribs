package com.dascribs.coreauth.controller.AuthAndUSer.auth;

import com.dascribs.coreauth.dto.shared.ApiResponse;
import com.dascribs.coreauth.dto.shared.SessionResponse;
import com.dascribs.coreauth.entity.user.UserSession;
import com.dascribs.coreauth.service.auth.SessionService;
import com.dascribs.coreauth.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getUserSessions() {
        try {
            Long userId = userService.getCurrentUserDetails().getId();
            List<UserSession> sessions = sessionService.getUserSessions(userId);

            List<SessionResponse> response = sessions.stream()
                    .map(this::convertToSessionResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Sessions retrieved successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> terminateSession(@PathVariable Long sessionId) {
        try {
            sessionService.logoutSession(sessionId.toString());
            return ResponseEntity.ok(ApiResponse.success("Session terminated successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    private SessionResponse convertToSessionResponse(UserSession session) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setSessionToken(session.getSessionToken().substring(0, 8) + "...");
        response.setIpAddress(session.getIpAddress());
        response.setUserAgent(session.getUserAgent());
        response.setExpiresAt(session.getExpiresAt());
        response.setLastActivityAt(session.getLastActivityAt());
        response.setCreatedAt(session.getCreatedAt());
        response.setActive(session.isValid());
        response.setCurrentSession(false);

        return response;
    }
}