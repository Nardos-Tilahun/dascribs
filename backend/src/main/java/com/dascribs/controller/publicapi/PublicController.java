package com.dascribs.controller.publicapi;

import com.dascribs.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        Map<String, String> healthStatus = Map.of(
                "status", "UP",
                "service", "DaScribs API",
                "version", "1.0.0",
                "timestamp", java.time.LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(ApiResponse.success("Service is healthy", healthStatus));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceInfo() {
        Map<String, Object> serviceInfo = Map.of(
                "name", "DaScribs Real Estate CRM",
                "version", "1.0.0",
                "description", "Comprehensive real estate customer relationship management system",
                "features", java.util.List.of(
                        "Multi-tenant Architecture",
                        "Role-based Access Control",
                        "Property Management",
                        "Lead & CRM Management",
                        "Appointment Scheduling",
                        "Deal & Transaction Management"
                ),
                "support", Map.of(
                        "email", "support@dascribs.com",
                        "website", "https://dascribs.com"
                )
        );

        return ResponseEntity.ok(ApiResponse.success("Service information retrieved", serviceInfo));
    }
}