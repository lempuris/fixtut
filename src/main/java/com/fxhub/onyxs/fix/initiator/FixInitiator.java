package com.fxhub.onyxs.fix.initiator;

import com.fxhub.onyxs.config.FixEngineConfig;
import com.fxhub.onyxs.models.Order;
import com.fxhub.onyxs.models.MarketDataQuote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import quickfix.*;
import quickfix.fix44.NewOrderSingle;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * FIX Initiator Component (Client) using QuickFIX/J
 * 
 * This component acts as a FIX client that:
 * - Initiates connections to FIX acceptors (exchanges)
 * - Sends orders and market data requests
 * - Receives and processes execution reports and market data
 * - Manages session connectivity and recovery
 */
@Slf4j
@Component
public class FixInitiator implements Application {
    
    @Autowired
    private FixEngineConfig config;
    
    private SocketInitiator initiator;
    private SessionID sessionID;
    private boolean isConnected = false;
    
    // Track pending orders for async responses
    private final Map<String, CompletableFuture<Order>> pendingOrders = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initialize() {
        if (!config.getInitiator().isEnabled()) {
            log.info("FIX Initiator is disabled in configuration");
            return;
        }
        
        try {
            log.info("Initializing FIX Initiator (Client) using QuickFIX/J...");
            
            // Create necessary directories
            createDirectories();
            
            // Create settings
            SessionSettings settings = createSessionSettings();
            
            // Create session ID
            sessionID = new SessionID(
                "FIX.4.4",
                config.getInitiator().getSenderCompId(),
                config.getInitiator().getTargetCompId()
            );
            
            // Create message store factory
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            
            // Create log factory
            LogFactory logFactory = new FileLogFactory(settings);
            
            // Create message factory
            MessageFactory messageFactory = new DefaultMessageFactory();
            
            // Create initiator
            initiator = new SocketInitiator(this, storeFactory, settings, logFactory, messageFactory);
            
            log.info("FIX Initiator initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize FIX Initiator", e);
            throw new RuntimeException("FIX Initiator initialization failed", e);
        }
    }
    
    /**
     * Create necessary directories for FIX storage
     */
    private void createDirectories() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(config.getInitiator().getStorePath()));
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(config.getInitiator().getLogPath()));
            log.debug("Created FIX initiator directories");
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
        settings.setString("ConnectionType", "initiator");
        settings.setString("StartTime", "00:00:00");
        settings.setString("EndTime", "00:00:00");
        settings.setString("HeartBtInt", String.valueOf(config.getHeartbeatInterval()));
        settings.setString("ReconnectInterval", String.valueOf(config.getInitiator().getReconnectInterval()));
        settings.setString("UseDataDictionary", "Y");
        settings.setString("DataDictionary", "FIX44.xml");
        
        // Session specific settings
        SessionID sid = new SessionID(
            "FIX.4.4",
            config.getInitiator().getSenderCompId(),
            config.getInitiator().getTargetCompId()
        );
        
        settings.setString(sid, "BeginString", "FIX.4.4");
        settings.setString(sid, "SenderCompID", config.getInitiator().getSenderCompId());
        settings.setString(sid, "TargetCompID", config.getInitiator().getTargetCompId());
        settings.setString(sid, "SocketConnectHost", config.getInitiator().getHost());
        settings.setString(sid, "SocketConnectPort", String.valueOf(config.getInitiator().getPort()));
        settings.setString(sid, "FileStorePath", config.getInitiator().getStorePath());
        settings.setString(sid, "FileLogPath", config.getInitiator().getLogPath());
        
        return settings;
    }
    
    /**
     * Connect to the FIX server
     */
    public void connect() {
        try {
            if (initiator != null && !isConnected) {
                initiator.start();
                log.info("FIX Initiator connecting to {}:{}", 
                    config.getInitiator().getHost(), config.getInitiator().getPort());
            }
        } catch (Exception e) {
            log.error("Failed to connect", e);
        }
    }
    
    /**
     * Disconnect from the FIX server
     */
    public void disconnect() {
        try {
            if (initiator != null && isConnected) {
                initiator.stop();
                log.info("FIX Initiator disconnecting");
            }
        } catch (Exception e) {
            log.error("Failed to disconnect", e);
        }
    }
    
    /**
     * Send a new order to the exchange
     */
    public CompletableFuture<Order> sendOrder(Order order) {
        CompletableFuture<Order> future = new CompletableFuture<>();
        
        if (!isConnected) {
            future.completeExceptionally(new Exception("Not connected to FIX server"));
            return future;
        }
        
        try {
            NewOrderSingle newOrder = new NewOrderSingle();
            
            // Set required fields
            newOrder.set(new quickfix.field.ClOrdID(order.getClOrdID()));
            newOrder.set(new quickfix.field.Symbol(order.getSymbol()));
            newOrder.set(new quickfix.field.Side(order.getSide().getFixValue()));
            newOrder.set(new quickfix.field.OrderQty(order.getOrderQty().doubleValue()));
            newOrder.set(new quickfix.field.OrdType(order.getOrderType().getFixValue()));
            newOrder.set(new quickfix.field.TransactTime()); // Required field - tag 60
            
            // Set optional fields
            if (order.getPrice() != null) {
                newOrder.set(new quickfix.field.Price(order.getPrice().doubleValue()));
            }
            if (order.getTimeInForce() != null) {
                newOrder.set(new quickfix.field.TimeInForce(order.getTimeInForce().getFixValue()));
            }
            
            // Track pending order
            pendingOrders.put(order.getClOrdID(), future);
            
            // Send the order
            Session session = Session.lookupSession(sessionID);
            if (session != null) {
                session.send(newOrder);
                log.info("Sent new order: {}", order.getClOrdID());
            }
            
        } catch (Exception e) {
            log.error("Failed to send order", e);
            future.completeExceptionally(e);
            pendingOrders.remove(order.getClOrdID());
        }
        
        return future;
    }
    
    /**
     * Cancel an existing order
     */
    public void cancelOrder(String clOrdID, String origClOrdID) {
        if (!isConnected) {
            log.error("Cannot cancel order - not connected");
            return;
        }
        
        try {
            quickfix.fix44.OrderCancelRequest cancelRequest = new quickfix.fix44.OrderCancelRequest();
            cancelRequest.set(new quickfix.field.ClOrdID(clOrdID));
            cancelRequest.set(new quickfix.field.OrigClOrdID(origClOrdID));
            cancelRequest.set(new quickfix.field.Symbol("N/A")); // Required field
            cancelRequest.set(new quickfix.field.Side(quickfix.field.Side.BUY)); // Required field
            cancelRequest.set(new quickfix.field.TransactTime());
            
            Session session = Session.lookupSession(sessionID);
            if (session != null) {
                session.send(cancelRequest);
                log.info("Sent cancel request for order {}", origClOrdID);
            }
            
        } catch (Exception e) {
            log.error("Failed to send cancel request", e);
        }
    }
    
    /**
     * Request market data for symbols
     */
    public CompletableFuture<MarketDataQuote> requestMarketData(String symbol) {
        CompletableFuture<MarketDataQuote> future = new CompletableFuture<>();
        
        if (!isConnected) {
            future.completeExceptionally(new Exception("Not connected to FIX server"));
            return future;
        }
        
        // Simplified - would implement market data request here
        future.complete(MarketDataQuote.builder()
            .symbol(symbol)
            .bidPx(new BigDecimal("1.0850"))
            .offerPx(new BigDecimal("1.0852"))
            .build());
        
        return future;
    }
    
    // QuickFIX/J Application callbacks
    
    @Override
    public void onCreate(SessionID sessionId) {
        log.info("Session created: {}", sessionId);
    }
    
    @Override
    public void onLogon(SessionID sessionId) {
        log.info("Logged on to FIX server: {}", sessionId);
        isConnected = true;
    }
    
    @Override
    public void onLogout(SessionID sessionId) {
        log.info("Logged out from FIX server: {}", sessionId);
        isConnected = false;
        
        // Cancel all pending orders
        pendingOrders.values().forEach(future -> 
            future.completeExceptionally(new Exception("Connection lost")));
        pendingOrders.clear();
    }
    
    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // Can modify outgoing admin messages here
    }
    
    @Override
    public void fromAdmin(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // Handle incoming admin messages
    }
    
    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        // Can modify outgoing application messages here
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionId) 
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        
        try {
            String msgType = message.getHeader().getString(35);
            
            if (msgType.equals("8")) { // Execution Report
                handleExecutionReport(message);
            }
            
        } catch (Exception e) {
            log.error("Error handling application message", e);
        }
    }
    
    /**
     * Handle Execution Report
     */
    private void handleExecutionReport(Message message) {
        try {
            String clOrdID = message.getString(11);
            String orderID = message.getString(37);
            char ordStatus = message.getChar(39);
            
            log.info("Execution Report received - ClOrdID: {}, Status: {}", clOrdID, ordStatus);
            
            // Find pending order future
            CompletableFuture<Order> future = pendingOrders.get(clOrdID);
            if (future != null) {
                Order order = Order.builder()
                    .clOrdID(clOrdID)
                    .orderID(orderID)
                    .status(Order.OrderStatus.fromFixValue(ordStatus))
                    .build();
                
                future.complete(order);
                
                // Remove from pending if order is complete
                if (isOrderComplete(ordStatus)) {
                    pendingOrders.remove(clOrdID);
                }
            }
            
        } catch (Exception e) {
            log.error("Error handling Execution Report", e);
        }
    }
    
    /**
     * Check if order is in a terminal state
     */
    private boolean isOrderComplete(char ordStatus) {
        return ordStatus == '2' || // Filled
               ordStatus == '4' || // Canceled
               ordStatus == '8' || // Rejected
               ordStatus == 'C';   // Expired
    }
    
    /**
     * Get connection status
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Get session state
     */
    public String getSessionState() {
        return isConnected ? "CONNECTED" : "DISCONNECTED";
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down FIX Initiator...");
        
        try {
            if (initiator != null) {
                initiator.stop();
            }
            
            log.info("FIX Initiator shutdown complete");
            
        } catch (Exception e) {
            log.error("Error during FIX Initiator shutdown", e);
        }
    }
}