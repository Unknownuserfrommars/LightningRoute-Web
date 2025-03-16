package com.lightningroute.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health check endpoints
 */
@RestController
@RequestMapping("/api/mindmap")
public class HealthCheckController {

    /**
     * Health check endpoint to verify the API is running
     * 
     * @return Health status information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Mind Map Generator");
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }
}
