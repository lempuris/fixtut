package com.fxhub.onyxs.controllers;

import com.fxhub.onyxs.fix.acceptor.FixAcceptor;
import com.fxhub.onyxs.fix.initiator.FixInitiator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * FIX Connection Controller
 * REST API endpoints for managing FIX connections
 */
@Slf4j
@RestController
@RequestMapping("/api/fix")
public class FixConnectionController {
    
    @Autowired
    private FixInitiator fixInitiator;
    
    @Autowired
    private FixAcceptor fixAcceptor;
    
    /**
     * Get FIX connection status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Initiator status
        Map<String, Object> initiatorStatus = new HashMap<>();
        initiatorStatus.put("connected", fixInitiator.isConnected());
        initiatorStatus.put("state", fixInitiator.getSessionState());
        status.put("initiator", initiatorStatus);
        
        // Acceptor status
        Map<String, Object> acceptorStatus = new HashMap<>();
        acceptorStatus.put("activeConnections", fixAcceptor.getActiveConnectionCount());
        status.put("acceptor", acceptorStatus);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * Connect the FIX initiator
     */
    @PostMapping("/initiator/connect")
    public ResponseEntity<?> connectInitiator() {
        try {
            if (fixInitiator.isConnected()) {
                return ResponseEntity.ok("Already connected");
            }
            
            fixInitiator.connect();
            
            // Wait a bit for connection to establish
            Thread.sleep(1000);
            
            if (fixInitiator.isConnected()) {
                return ResponseEntity.ok("Connected successfully");
            } else {
                return ResponseEntity.status(503).body("Connection in progress");
            }
            
        } catch (Exception e) {
            log.error("Error connecting initiator", e);
            return ResponseEntity.status(500).body("Failed to connect: " + e.getMessage());
        }
    }
    
    /**
     * Disconnect the FIX initiator
     */
    @PostMapping("/initiator/disconnect")
    public ResponseEntity<?> disconnectInitiator() {
        try {
            if (!fixInitiator.isConnected()) {
                return ResponseEntity.ok("Already disconnected");
            }
            
            fixInitiator.disconnect();
            return ResponseEntity.ok("Disconnected successfully");
            
        } catch (Exception e) {
            log.error("Error disconnecting initiator", e);
            return ResponseEntity.status(500).body("Failed to disconnect: " + e.getMessage());
        }
    }
    
    /**
     * Check if a specific client is connected to acceptor
     */
    @GetMapping("/acceptor/client/{compId}")
    public ResponseEntity<Map<String, Object>> checkClientConnection(@PathVariable String compId) {
        Map<String, Object> result = new HashMap<>();
        result.put("compId", compId);
        result.put("connected", fixAcceptor.isClientConnected(compId));
        return ResponseEntity.ok(result);
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "FIX Engine");
        return ResponseEntity.ok(health);
    }
}