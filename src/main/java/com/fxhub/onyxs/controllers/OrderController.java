package com.fxhub.onyxs.controllers;

import com.fxhub.onyxs.fix.initiator.FixInitiator;
import com.fxhub.onyxs.models.Order;
import com.fxhub.onyxs.services.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Order Controller
 * REST API endpoints for order management and testing FIX functionality
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private FixInitiator fixInitiator;
    
    /**
     * Submit a new order via FIX
     */
    @PostMapping
    public ResponseEntity<?> submitOrder(@RequestBody OrderRequest request) {
        try {
            // Validate request
            if (request.getSymbol() == null || request.getQuantity() == null) {
                return ResponseEntity.badRequest().body("Symbol and quantity are required");
            }
            
            // Create order
            Order order = Order.builder()
                .clOrdID(generateClOrdID())
                .symbol(request.getSymbol())
                .side(request.getSide() != null ? request.getSide() : Order.OrderSide.BUY)
                .orderType(request.getOrderType() != null ? request.getOrderType() : Order.OrderType.MARKET)
                .orderQty(request.getQuantity())
                .price(request.getPrice())
                .timeInForce(request.getTimeInForce() != null ? request.getTimeInForce() : Order.TimeInForce.DAY)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .transactTime(LocalDateTime.now())
                .build();
            
            // Check if FIX initiator is connected
            if (!fixInitiator.isConnected()) {
                log.warn("FIX initiator not connected, storing order locally");
                order.setStatus(Order.OrderStatus.PENDING_NEW);
                orderService.submitOrder(order);
                return ResponseEntity.accepted().body(order);
            }
            
            // Send order via FIX (fire and forget for demo)
            CompletableFuture<Order> future = fixInitiator.sendOrder(order);
            
            // For demo purposes, simulate immediate acknowledgment
            order.setStatus(Order.OrderStatus.NEW);
            order.setOrderID("ORD" + System.currentTimeMillis());
            orderService.submitOrder(order);
            
            // Simulate market order fill after a short delay
            if (order.getOrderType() == Order.OrderType.MARKET) {
                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        order.setStatus(Order.OrderStatus.FILLED);
                        order.setExecutedQty(order.getOrderQty());
                        order.setAvgExecutedPrice(new BigDecimal("1.0850"));
                        orderService.updateOrder(order);
                        log.info("Order {} filled at {}", order.getClOrdID(), order.getAvgExecutedPrice());
                    } catch (Exception e) {
                        log.error("Error simulating fill", e);
                    }
                }).start();
            }
            
            return ResponseEntity.ok(order);
            
        } catch (Exception e) {
            log.error("Error submitting order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to submit order: " + e.getMessage());
        }
    }
    
    /**
     * Get all orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
    
    /**
     * Get order by Client Order ID
     */
    @GetMapping("/{clOrdID}")
    public ResponseEntity<?> getOrder(@PathVariable String clOrdID) {
        Order order = orderService.findByClOrdID(clOrdID);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }
    
    /**
     * Get active orders
     */
    @GetMapping("/active")
    public ResponseEntity<List<Order>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }
    
    /**
     * Get orders by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(orderService.getOrdersByStatus(orderStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Cancel an order
     */
    @DeleteMapping("/{clOrdID}")
    public ResponseEntity<?> cancelOrder(@PathVariable String clOrdID) {
        try {
            // Find the order
            Order order = orderService.findByClOrdID(clOrdID);
            if (order == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Send cancel request via FIX if connected
            if (fixInitiator.isConnected()) {
                String cancelClOrdID = generateClOrdID();
                fixInitiator.cancelOrder(cancelClOrdID, clOrdID);
                return ResponseEntity.ok("Cancel request sent for order " + clOrdID);
            } else {
                // Cancel locally
                boolean canceled = orderService.cancelOrder(clOrdID);
                if (canceled) {
                    return ResponseEntity.ok("Order canceled locally");
                } else {
                    return ResponseEntity.badRequest().body("Could not cancel order");
                }
            }
            
        } catch (Exception e) {
            log.error("Error canceling order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to cancel order: " + e.getMessage());
        }
    }
    
    /**
     * Get order statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<OrderService.OrderStatistics> getStatistics() {
        return ResponseEntity.ok(orderService.getStatistics());
    }
    
    /**
     * Submit test orders for demonstration
     */
    @PostMapping("/test/submit-samples")
    public ResponseEntity<?> submitSampleOrders() {
        try {
            // Create sample orders
            Order[] sampleOrders = {
                Order.builder()
                    .clOrdID(generateClOrdID())
                    .symbol("EUR/USD")
                    .side(Order.OrderSide.BUY)
                    .orderType(Order.OrderType.MARKET)
                    .orderQty(new BigDecimal("100000"))
                    .currency("USD")
                    .build(),
                    
                Order.builder()
                    .clOrdID(generateClOrdID())
                    .symbol("GBP/USD")
                    .side(Order.OrderSide.SELL)
                    .orderType(Order.OrderType.LIMIT)
                    .orderQty(new BigDecimal("50000"))
                    .price(new BigDecimal("1.2650"))
                    .timeInForce(Order.TimeInForce.GTC)
                    .currency("USD")
                    .build(),
                    
                Order.builder()
                    .clOrdID(generateClOrdID())
                    .symbol("XAU/USD")
                    .side(Order.OrderSide.BUY)
                    .orderType(Order.OrderType.STOP)
                    .orderQty(new BigDecimal("10"))
                    .stopPx(new BigDecimal("2055.00"))
                    .currency("USD")
                    .build()
            };
            
            // Submit orders
            for (Order order : sampleOrders) {
                order.setTransactTime(LocalDateTime.now());
                
                if (fixInitiator.isConnected()) {
                    fixInitiator.sendOrder(order);
                } else {
                    order.setStatus(Order.OrderStatus.PENDING_NEW);
                    orderService.submitOrder(order);
                }
            }
            
            return ResponseEntity.ok("Submitted " + sampleOrders.length + " sample orders");
            
        } catch (Exception e) {
            log.error("Error submitting sample orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to submit sample orders: " + e.getMessage());
        }
    }
    
    /**
     * Clear all orders (for testing)
     */
    @DeleteMapping("/test/clear-all")
    public ResponseEntity<?> clearAllOrders() {
        orderService.clearAllOrders();
        return ResponseEntity.ok("All orders cleared");
    }
    
    /**
     * Generate unique Client Order ID
     */
    private String generateClOrdID() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Order Request DTO
     */
    public static class OrderRequest {
        private String symbol;
        private Order.OrderSide side;
        private Order.OrderType orderType;
        private BigDecimal quantity;
        private BigDecimal price;
        private BigDecimal stopPrice;
        private Order.TimeInForce timeInForce;
        private String currency;
        private String account;
        
        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        
        public Order.OrderSide getSide() { return side; }
        public void setSide(Order.OrderSide side) { this.side = side; }
        
        public Order.OrderType getOrderType() { return orderType; }
        public void setOrderType(Order.OrderType orderType) { this.orderType = orderType; }
        
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        
        public BigDecimal getStopPrice() { return stopPrice; }
        public void setStopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; }
        
        public Order.TimeInForce getTimeInForce() { return timeInForce; }
        public void setTimeInForce(Order.TimeInForce timeInForce) { this.timeInForce = timeInForce; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }
    }
}