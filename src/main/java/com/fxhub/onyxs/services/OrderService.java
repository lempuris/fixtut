package com.fxhub.onyxs.services;

import com.fxhub.onyxs.models.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Order Service
 * Manages order lifecycle and provides order management operations
 */
@Slf4j
@Service
public class OrderService {
    
    // In-memory order storage (in production, use a database)
    private final Map<String, Order> ordersByClOrdID = new ConcurrentHashMap<>();
    private final Map<String, Order> ordersByOrderID = new ConcurrentHashMap<>();
    
    /**
     * Submit a new order
     */
    public Order submitOrder(Order order) {
        // Set timestamps
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        
        // Store order
        ordersByClOrdID.put(order.getClOrdID(), order);
        if (order.getOrderID() != null) {
            ordersByOrderID.put(order.getOrderID(), order);
        }
        
        log.info("Order submitted: {} - {} {} @ {}", 
            order.getClOrdID(), 
            order.getSide(), 
            order.getOrderQty(), 
            order.getSymbol());
        
        return order;
    }
    
    /**
     * Update an existing order
     */
    public Order updateOrder(Order order) {
        order.setUpdatedAt(LocalDateTime.now());
        
        // Update in both maps
        ordersByClOrdID.put(order.getClOrdID(), order);
        if (order.getOrderID() != null) {
            ordersByOrderID.put(order.getOrderID(), order);
        }
        
        log.info("Order updated: {} - Status: {}", order.getClOrdID(), order.getStatus());
        
        return order;
    }
    
    /**
     * Find order by Client Order ID
     */
    public Order findByClOrdID(String clOrdID) {
        return ordersByClOrdID.get(clOrdID);
    }
    
    /**
     * Find order by Exchange Order ID
     */
    public Order findByOrderID(String orderID) {
        return ordersByOrderID.get(orderID);
    }
    
    /**
     * Get all orders
     */
    public List<Order> getAllOrders() {
        return new ArrayList<>(ordersByClOrdID.values());
    }
    
    /**
     * Get orders by status
     */
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return ordersByClOrdID.values().stream()
            .filter(order -> order.getStatus() == status)
            .collect(Collectors.toList());
    }
    
    /**
     * Get orders by symbol
     */
    public List<Order> getOrdersBySymbol(String symbol) {
        return ordersByClOrdID.values().stream()
            .filter(order -> symbol.equals(order.getSymbol()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get active orders (not filled, canceled, or rejected)
     */
    public List<Order> getActiveOrders() {
        return ordersByClOrdID.values().stream()
            .filter(order -> 
                order.getStatus() != Order.OrderStatus.FILLED &&
                order.getStatus() != Order.OrderStatus.CANCELED &&
                order.getStatus() != Order.OrderStatus.REJECTED &&
                order.getStatus() != Order.OrderStatus.EXPIRED)
            .collect(Collectors.toList());
    }
    
    /**
     * Cancel an order
     */
    public boolean cancelOrder(String clOrdID) {
        Order order = findByClOrdID(clOrdID);
        if (order == null) {
            log.warn("Order not found for cancel: {}", clOrdID);
            return false;
        }
        
        // Check if order can be canceled
        if (order.getStatus() == Order.OrderStatus.FILLED) {
            log.warn("Cannot cancel filled order: {}", clOrdID);
            return false;
        }
        
        order.setStatus(Order.OrderStatus.CANCELED);
        updateOrder(order);
        
        log.info("Order canceled: {}", clOrdID);
        return true;
    }
    
    /**
     * Get order statistics
     */
    public OrderStatistics getStatistics() {
        OrderStatistics stats = new OrderStatistics();
        
        for (Order order : ordersByClOrdID.values()) {
            stats.totalOrders++;
            
            switch (order.getStatus()) {
                case NEW:
                case PENDING_NEW:
                    stats.newOrders++;
                    break;
                case PARTIALLY_FILLED:
                    stats.partiallyFilledOrders++;
                    break;
                case FILLED:
                    stats.filledOrders++;
                    break;
                case CANCELED:
                    stats.canceledOrders++;
                    break;
                case REJECTED:
                    stats.rejectedOrders++;
                    break;
                default:
                    break;
            }
            
            // Count by side
            if (order.getSide() == Order.OrderSide.BUY) {
                stats.buyOrders++;
            } else {
                stats.sellOrders++;
            }
        }
        
        return stats;
    }
    
    /**
     * Clear all orders (for testing)
     */
    public void clearAllOrders() {
        ordersByClOrdID.clear();
        ordersByOrderID.clear();
        log.info("All orders cleared");
    }
    
    /**
     * Order statistics class
     */
    public static class OrderStatistics {
        public int totalOrders = 0;
        public int newOrders = 0;
        public int partiallyFilledOrders = 0;
        public int filledOrders = 0;
        public int canceledOrders = 0;
        public int rejectedOrders = 0;
        public int buyOrders = 0;
        public int sellOrders = 0;
        
        @Override
        public String toString() {
            return String.format(
                "Total: %d, New: %d, PartialFill: %d, Filled: %d, Canceled: %d, Rejected: %d, Buy: %d, Sell: %d",
                totalOrders, newOrders, partiallyFilledOrders, filledOrders, 
                canceledOrders, rejectedOrders, buyOrders, sellOrders
            );
        }
    }
}