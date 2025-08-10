package com.fxhub.onyxs.fix.acceptor;

import com.fxhub.onyxs.config.FixEngineConfig;
import com.fxhub.onyxs.fix.handlers.AcceptorMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import quickfix.*;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * FIX Acceptor Component (Exchange Simulator) using QuickFIX/J
 * 
 * This component simulates an exchange that:
 * - Accepts FIX connections from clients
 * - Receives and processes orders (NewOrderSingle)
 * - Sends back execution reports
 * - Manages market data quotes
 * 
 * Key FIX Messages handled:
 * - Logon (35=A): Session establishment
 * - Logout (35=5): Session termination  
 * - Heartbeat (35=0): Keep-alive messages
 * - New Order Single (35=D): New order submission
 * - Order Cancel Request (35=F): Cancel an existing order
 */
@Slf4j
@Component
public class FixAcceptor implements Application {
    
    @Autowired
    private FixEngineConfig config;
    
    @Autowired
    private AcceptorMessageHandler messageHandler;
    
    private SocketAcceptor acceptor;
    private final Map<SessionID, Session> activeSessions = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        if (!config.getAcceptor().isEnabled()) {
            log.info("FIX Acceptor is disabled in configuration");
            return;
        }
        
        try {
            log.info("Initializing FIX Acceptor (Exchange Simulator) using QuickFIX/J...");
            
            // Create necessary directories
            createDirectories();
            
            // Create settings
            SessionSettings settings = createSessionSettings();
            
            // Create message store factory
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            
            // Create log factory
            LogFactory logFactory = new FileLogFactory(settings);
            
            // Create message factory
            MessageFactory messageFactory = new DefaultMessageFactory();
            
            // Create acceptor
            acceptor = new SocketAcceptor(this, storeFactory, settings, logFactory, messageFactory);
            
            // Start the acceptor immediately
            acceptor.start();
            
            log.info("FIX Acceptor started and listening on port {}", config.getAcceptor().getPort());
            
        } catch (Exception e) {
            log.error("Failed to initialize FIX Acceptor", e);
            throw new RuntimeException("FIX Acceptor initialization failed", e);
        }
    }
    
    /**
     * Create necessary directories for FIX storage
     */
    private void createDirectories() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(config.getAcceptor().getStorePath()));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(config.getAcceptor().getLogPath()));
            log.debug("Created FIX acceptor directories");
        } catch (Exception e) {
            log.warn("Could not create directories: {}", e.getMessage());
        }
    }
    
    /**
     * Creates QuickFIX/J session settings
     */
    private SessionSettings createSessionSettings() {
        SessionSettings settings = new SessionSettings();
        
        // Default settings
        settings.setString("ConnectionType", "acceptor");
        settings.setString("SocketAcceptPort", String.valueOf(config.getAcceptor().getPort()));
        settings.setString("StartTime", "00:00:00");
        settings.setString("EndTime", "00:00:00");
        settings.setString("HeartBtInt", String.valueOf(config.getHeartbeatInterval()));
        settings.setString("ValidOrderTypes", "1,2,3,4,F");
        settings.setString("UseDataDictionary", "Y");
        settings.setString("DataDictionary", "FIX44.xml");
        
        // Session specific settings
        SessionID sessionID = new SessionID(
            "FIX.4.4",
            config.getAcceptor().getSenderCompId(),
            config.getAcceptor().getTargetCompId()
        );
        
        settings.setString(sessionID, "BeginString", "FIX.4.4");
        settings.setString(sessionID, "SenderCompID", config.getAcceptor().getSenderCompId());
        settings.setString(sessionID, "TargetCompID", config.getAcceptor().getTargetCompId());
        settings.setString(sessionID, "FileStorePath", config.getAcceptor().getStorePath());
        settings.setString(sessionID, "FileLogPath", config.getAcceptor().getLogPath());
        
        return settings;
    }
    
    /**
     * Start the acceptor
     */
    public void start() {
        try {
            if (acceptor != null) {
                acceptor.start();
                log.info("FIX Acceptor started");
            }
        } catch (Exception e) {
            log.error("Failed to start FIX Acceptor", e);
        }
    }
    
    /**
     * QuickFIX/J Application callback - called when session is created
     */
    @Override
    public void onCreate(SessionID sessionId) {
        log.info("Session created: {}", sessionId);
    }
    
    /**
     * QuickFIX/J Application callback - called on logon
     */
    @Override
    public void onLogon(SessionID sessionId) {
        log.info("Logon received from: {}", sessionId);
        Session session = Session.lookupSession(sessionId);
        if (session != null) {
            activeSessions.put(sessionId, session);
        }
    }
    
    /**
     * QuickFIX/J Application callback - called on logout
     */
    @Override
    public void onLogout(SessionID sessionId) {
        log.info("Logout received from: {}", sessionId);
        activeSessions.remove(sessionId);
    }
    
    /**
     * QuickFIX/J Application callback - admin messages
     */
    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // Can modify outgoing admin messages here
    }
    
    /**
     * QuickFIX/J Application callback - admin messages
     */
    @Override
    public void fromAdmin(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // Handle incoming admin messages
    }
    
    /**
     * QuickFIX/J Application callback - application messages to send
     */
    @Override
    public void toApp(Message message, SessionID sessionId) 
            throws DoNotSend {
        // Can modify outgoing application messages here
    }
    
    /**
     * QuickFIX/J Application callback - incoming application messages
     */
    @Override
    public void fromApp(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        
        try {
            String msgType = message.getHeader().getString(35);
            log.info("Received application message type: {} from {}", msgType, sessionId);
            
            // Handle different message types
            if (msgType.equals("D")) { // New Order Single
                log.info("Processing New Order Single message");
                handleNewOrderSingle((NewOrderSingle) message, sessionId);
            } else {
                log.warn("Unhandled message type: {}", msgType);
            }
            
        } catch (Exception e) {
            log.error("Error handling application message", e);
        }
    }
    
    /**
     * Handle New Order Single
     */
    private void handleNewOrderSingle(NewOrderSingle order, SessionID sessionId) {
        try {
            String clOrdID = order.getClOrdID().getValue();
            String symbol = order.getSymbol().getValue();
            
            log.info("Processing New Order Single: {} for {}", clOrdID, symbol);
            
            // Create execution report (acknowledgment)
            ExecutionReport execReport = new ExecutionReport();
            execReport.set(new quickfix.field.OrderID("ORD" + System.currentTimeMillis()));
            execReport.set(new quickfix.field.ExecID("EXEC" + System.currentTimeMillis()));
            execReport.set(new quickfix.field.ExecType(quickfix.field.ExecType.NEW));
            execReport.set(new quickfix.field.OrdStatus(quickfix.field.OrdStatus.NEW));
            execReport.set(order.getClOrdID());
            execReport.set(order.getSymbol());
            execReport.set(order.getSide());
            execReport.set(order.getOrderQty());
            execReport.set(new quickfix.field.CumQty(0));
            execReport.set(new quickfix.field.AvgPx(0));
            execReport.set(new quickfix.field.LeavesQty(order.getOrderQty().getValue()));
            
            // Send the execution report back - swap sender and target for the response
            try {
                SessionID responseSessionID = new SessionID(
                    sessionId.getBeginString(),
                    sessionId.getTargetCompID(),  // Swap - target becomes sender
                    sessionId.getSenderCompID()   // Swap - sender becomes target
                );
                Session.sendToTarget(execReport, responseSessionID);
                log.info("Sent execution report for order {} to {}", clOrdID, responseSessionID);
                
                // For market orders, send immediate fill
                if (order.getOrdType().getValue() == quickfix.field.OrdType.MARKET) {
                    Thread.sleep(100); // Small delay to simulate processing
                    
                    ExecutionReport fillReport = new ExecutionReport();
                    fillReport.set(new quickfix.field.OrderID("ORD" + System.currentTimeMillis()));
                    fillReport.set(new quickfix.field.ExecID("EXEC" + System.currentTimeMillis()));
                    fillReport.set(new quickfix.field.ExecType(quickfix.field.ExecType.TRADE));
                    fillReport.set(new quickfix.field.OrdStatus(quickfix.field.OrdStatus.FILLED));
                    fillReport.set(order.getClOrdID());
                    fillReport.set(order.getSymbol());
                    fillReport.set(order.getSide());
                    fillReport.set(order.getOrderQty());
                    fillReport.set(new quickfix.field.CumQty(order.getOrderQty().getValue()));
                    fillReport.set(new quickfix.field.AvgPx(1.0850)); // Simulated price
                    fillReport.set(new quickfix.field.LastPx(1.0850));
                    fillReport.set(new quickfix.field.LastQty(order.getOrderQty().getValue()));
                    fillReport.set(new quickfix.field.LeavesQty(0));
                    
                    Session.sendToTarget(fillReport, responseSessionID);
                    log.info("Sent fill execution report for market order {}", clOrdID);
                }
            } catch (SessionNotFound e) {
                log.error("Session not found: {}", sessionId);
            }
            
        } catch (Exception e) {
            log.error("Error handling New Order Single", e);
        }
    }
    
    /**
     * Get the number of active connections
     */
    public int getActiveConnectionCount() {
        return activeSessions.size();
    }
    
    /**
     * Check if a specific client is connected
     */
    public boolean isClientConnected(String targetCompId) {
        return activeSessions.keySet().stream()
            .anyMatch(sid -> sid.getTargetCompID().equals(targetCompId));
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down FIX Acceptor...");
        
        try {
            if (acceptor != null) {
                acceptor.stop();
            }
            
            log.info("FIX Acceptor shutdown complete");
            
        } catch (Exception e) {
            log.error("Error during FIX Acceptor shutdown", e);
        }
    }
}