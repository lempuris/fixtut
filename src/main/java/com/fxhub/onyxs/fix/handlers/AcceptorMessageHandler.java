package com.fxhub.onyxs.fix.handlers;

import com.fxhub.onyxs.models.Order;
import com.fxhub.onyxs.services.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Message Handler for FIX Acceptor (Exchange side)
 * Processes incoming FIX messages and generates appropriate responses
 * 
 * This is a simplified handler that works with the QuickFIX/J implementation
 */
@Slf4j
@Component
public class AcceptorMessageHandler {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * Process a new order
     */
    public Order processNewOrder(String clOrdID, String symbol, char side, 
                                 BigDecimal orderQty, char ordType, BigDecimal price) {
        
        log.info("Processing new order: {} for {} {}", clOrdID, orderQty, symbol);
        
        // Create order object
        Order order = Order.builder()
            .clOrdID(clOrdID)
            .orderID(generateOrderID())
            .symbol(symbol)
            .side(Order.OrderSide.fromFixValue(side))
            .orderType(Order.OrderType.fromFixValue(ordType))
            .orderQty(orderQty)
            .price(price)
            .status(Order.OrderStatus.NEW)
            .transactTime(LocalDateTime.now())
            .createdAt(LocalDateTime.now())
            .build();
        
        // Process order through service
        return orderService.submitOrder(order);
    }
    
    /**
     * Process order cancel request
     */
    public boolean processCancelRequest(String origClOrdID) {
        log.info("Processing cancel request for order {}", origClOrdID);
        
        Order order = orderService.findByClOrdID(origClOrdID);
        if (order == null) {
            log.warn("Order not found: {}", origClOrdID);
            return false;
        }
        
        if (order.getStatus() == Order.OrderStatus.FILLED) {
            log.warn("Cannot cancel filled order: {}", origClOrdID);
            return false;
        }
        
        order.setStatus(Order.OrderStatus.CANCELED);
        orderService.updateOrder(order);
        return true;
    }
    
    /**
     * Get simulated market price for a symbol
     */
    public BigDecimal getMarketPrice(String symbol) {
        // Simulated market prices
        switch (symbol) {
            case "EUR/USD":
                return new BigDecimal("1.0856");
            case "GBP/USD":
                return new BigDecimal("1.2634");
            case "USD/JPY":
                return new BigDecimal("150.25");
            case "XAU/USD":
                return new BigDecimal("2050.50");
            default:
                return new BigDecimal("1.0000");
        }
    }
    
    private String generateOrderID() {
        return "ORD" + System.currentTimeMillis();
    }
}