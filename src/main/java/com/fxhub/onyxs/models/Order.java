package com.fxhub.onyxs.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order model representing a FIX New Order Single (35=D)
 * This model captures the essential fields for an order in FX/Gold trading
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    // Order identification
    private String clOrdID;           // Client Order ID (tag 11) - unique ID from client
    private String orderID;           // Exchange Order ID (tag 37) - assigned by exchange
    
    // Instrument details
    private String symbol;            // Symbol (tag 55) - e.g., EUR/USD, XAU/USD
    private String securityType;      // Security Type (tag 167) - FOR (Forex), CMDTY (Commodity)
    
    // Order details
    private OrderSide side;          // Side (tag 54) - Buy(1) or Sell(2)
    private OrderType orderType;     // Order Type (tag 40) - Market(1), Limit(2), Stop(3)
    private BigDecimal orderQty;     // Order Quantity (tag 38) - amount in base currency
    private BigDecimal price;        // Price (tag 44) - limit price for limit orders
    private BigDecimal stopPx;       // Stop Price (tag 99) - trigger price for stop orders
    
    // Execution instructions
    private TimeInForce timeInForce; // Time in Force (tag 59) - GTC, IOC, FOK, GTD
    private String currency;         // Currency (tag 15) - settlement currency
    
    // Status tracking
    private OrderStatus status;      // Current order status
    private BigDecimal executedQty;  // Cumulative executed quantity
    private BigDecimal avgExecutedPrice; // Average execution price
    
    // Timestamps
    private LocalDateTime transactTime;  // Transaction time (tag 60)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields
    private String account;          // Account (tag 1) - trading account
    private String text;             // Free text (tag 58) - additional information
    
    /**
     * Order Side enumeration matching FIX protocol
     */
    public enum OrderSide {
        BUY('1'),     // Buy order
        SELL('2'),    // Sell order
        BUY_MINUS('3'),
        SELL_PLUS('4'),
        SELL_SHORT('5'),
        SELL_SHORT_EXEMPT('6');
        
        private final char fixValue;
        
        OrderSide(char fixValue) {
            this.fixValue = fixValue;
        }
        
        public char getFixValue() {
            return fixValue;
        }
        
        public static OrderSide fromFixValue(char value) {
            for (OrderSide side : values()) {
                if (side.fixValue == value) {
                    return side;
                }
            }
            throw new IllegalArgumentException("Unknown FIX side value: " + value);
        }
    }
    
    /**
     * Order Type enumeration matching FIX protocol
     */
    public enum OrderType {
        MARKET('1'),           // Market order
        LIMIT('2'),            // Limit order
        STOP('3'),             // Stop order
        STOP_LIMIT('4'),       // Stop limit order
        MARKET_ON_CLOSE('5'),  // Market on close
        WITH_OR_WITHOUT('6'),
        LIMIT_OR_BETTER('7'),
        LIMIT_WITH_OR_WITHOUT('8'),
        ON_BASIS('9');
        
        private final char fixValue;
        
        OrderType(char fixValue) {
            this.fixValue = fixValue;
        }
        
        public char getFixValue() {
            return fixValue;
        }
        
        public static OrderType fromFixValue(char value) {
            for (OrderType type : values()) {
                if (type.fixValue == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown FIX order type value: " + value);
        }
    }
    
    /**
     * Time in Force enumeration matching FIX protocol
     */
    public enum TimeInForce {
        DAY('0'),              // Day order
        GTC('1'),              // Good Till Cancel
        IOC('3'),              // Immediate or Cancel
        FOK('4'),              // Fill or Kill
        GTX('5'),              // Good Till Crossing
        GTD('6');              // Good Till Date
        
        private final char fixValue;
        
        TimeInForce(char fixValue) {
            this.fixValue = fixValue;
        }
        
        public char getFixValue() {
            return fixValue;
        }
        
        public static TimeInForce fromFixValue(char value) {
            for (TimeInForce tif : values()) {
                if (tif.fixValue == value) {
                    return tif;
                }
            }
            throw new IllegalArgumentException("Unknown FIX TIF value: " + value);
        }
    }
    
    /**
     * Order Status enumeration
     */
    public enum OrderStatus {
        NEW('0'),              // New order
        PARTIALLY_FILLED('1'), // Partially filled
        FILLED('2'),           // Completely filled
        DONE_FOR_DAY('3'),     // Order not fully filled; no further executions for the day
        CANCELED('4'),         // Canceled
        REPLACED('5'),         // Replaced (Amended)
        PENDING_CANCEL('6'),   // Pending Cancel
        STOPPED('7'),          // Stopped
        REJECTED('8'),         // Rejected
        SUSPENDED('9'),        // Suspended
        PENDING_NEW('A'),      // Pending New
        CALCULATED('B'),       // Calculated
        EXPIRED('C'),          // Expired
        ACCEPTED_FOR_BIDDING('D'),
        PENDING_REPLACE('E'); // Pending Replace
        
        private final char fixValue;
        
        OrderStatus(char fixValue) {
            this.fixValue = fixValue;
        }
        
        public char getFixValue() {
            return fixValue;
        }
        
        public static OrderStatus fromFixValue(char value) {
            for (OrderStatus status : values()) {
                if (status.fixValue == value) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown FIX order status value: " + value);
        }
    }
}