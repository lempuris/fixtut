package com.fxhub.onyxs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * FIX Engine Configuration
 * Centralizes all FIX protocol settings for both acceptor and initiator
 */
@Configuration
@ConfigurationProperties(prefix = "fix.engine")
@Data
public class FixEngineConfig {
    
    // Engine settings
    private String licenseFile = "onyx-license.xml";
    private String fixVersion = "FIX.4.4";
    private String dataDirectory = "./fix-data";
    private boolean validateMessages = true;
    private int heartbeatInterval = 30;
    
    // Acceptor settings (Exchange simulator)
    private AcceptorConfig acceptor = new AcceptorConfig();
    
    // Initiator settings (Client)
    private InitiatorConfig initiator = new InitiatorConfig();
    
    // Performance settings
    private int threadPoolSize = 10;
    private int maxMessageSize = 10240; // 10KB
    private boolean enableLogging = true;
    
    @Data
    public static class AcceptorConfig {
        private boolean enabled = true;
        private int port = 9876;
        private String senderCompId = "EXCHANGE";
        private String targetCompId = "CLIENT";
        private String storePath = "./fix-store/acceptor";
        private String logPath = "./fix-logs/acceptor";
        private boolean resetOnLogon = false;
        private boolean resetOnDisconnect = false;
    }
    
    @Data
    public static class InitiatorConfig {
        private boolean enabled = true;
        private String host = "localhost";
        private int port = 9876;
        private String senderCompId = "CLIENT";
        private String targetCompId = "EXCHANGE";
        private String storePath = "./fix-store/initiator";
        private String logPath = "./fix-logs/initiator";
        private int reconnectInterval = 5;
        private boolean resetOnLogon = false;
        private boolean resetOnDisconnect = false;
    }
}