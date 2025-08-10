package com.fxhub.onyxs.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Execution Report model representing FIX Execution Report (35=8)
 * This is the primary message for order status updates and trade confirmations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionReport {
    
    // Report identification
    private String execID;            // Execution ID (tag 17) - unique execution identifier
    private String orderID;           // Order ID (tag 37) - exchange assigned order ID
    private String clOrdID;           // Client Order ID (tag 11) - client's order reference
    private String origClOrdID;       // Original Client Order ID (tag 41) - for cancel/replace
    
    // Execution details
    private ExecType execType;        // Execution Type (tag 150) - type of execution report
    private Order.OrderStatus ordStatus; // Order Status (tag 39) - current order status
    private ExecRestatementReason execRestatementReason; // Reason for restatement (tag 378)
    
    // Trade details
    private String execRefID;         // Execution Reference ID (tag 19) - reference to previous exec
    private BigDecimal lastPx;        // Last Price (tag 31) - price of this fill
    private BigDecimal lastQty;       // Last Quantity (tag 32) - quantity of this fill
    private BigDecimal cumQty;        // Cumulative Quantity (tag 14) - total filled quantity
    private BigDecimal avgPx;         // Average Price (tag 6) - average price of all fills
    private BigDecimal leavesQty;     // Leaves Quantity (tag 151) - remaining quantity
    
    // Order details (echoed back)
    private String symbol;            // Symbol (tag 55)
    private Order.OrderSide side;     // Side (tag 54)
    private Order.OrderType ordType;  // Order Type (tag 40)
    private BigDecimal orderQty;      // Order Quantity (tag 38)
    private BigDecimal price;         // Price (tag 44) - limit price
    private Order.TimeInForce timeInForce; // Time in Force (tag 59)
    
    // Settlement and fees
    private String currency;          // Currency (tag 15)
    private BigDecimal commission;    // Commission (tag 12)
    private String commType;          // Commission Type (tag 13)
    private BigDecimal netMoney;      // Net Money (tag 118) - for FX settlement
    private LocalDateTime settlDate;  // Settlement Date (tag 64)
    
    // Rejection details
    private Integer ordRejReason;     // Order Reject Reason (tag 103)
    private String text;              // Free Text (tag 58) - additional information
    
    // Timestamps
    private LocalDateTime transactTime; // Transaction Time (tag 60)
    private LocalDateTime sendingTime;  // Sending Time (tag 52)
    private LocalDateTime createdAt;
    
    // Additional FX specific fields
    private BigDecimal settlCurrAmt;  // Settlement Currency Amount (tag 119)
    private String settlCurrency;     // Settlement Currency (tag 120)
    private BigDecimal settlCurrFxRate; // Settlement Currency FX Rate (tag 155)
    
    /**
     * Execution Type enumeration matching FIX protocol
     */
    public enum ExecType {
        NEW('0'),              // New order acknowledgment
        PARTIAL_FILL('1'),     // Partial fill (deprecated, use TRADE)
        FILL('2'),             // Full fill (deprecated, use TRADE)  
        DONE_FOR_DAY('3'),     // Done for day
        CANCELED('4'),         // Order canceled
        REPLACE('5'),          // Order replaced/amended
        PENDING_CANCEL('6'),   // Pending cancel acknowledgment
        STOPPED('7'),          // Order stopped
        REJECTED('8'),         // Order rejected
        SUSPENDED('9'),        // Order suspended
        PENDING_NEW('A'),      // Pending new acknowledgment
        CALCULATED('B'),       // Calculated
        EXPIRED('C'),          // Order expired
        RESTATED('D'),         // Order restated
        PENDING_REPLACE('E'),  // Pending replace acknowledgment
        TRADE('F'),            // Trade (partial or full fill)
        TRADE_CORRECT('G'),    // Trade correction
        TRADE_CANCEL('H'),     // Trade cancel
        ORDER_STATUS('I');     // Order status request response
        
        private final char fixValue;
        
        ExecType(char fixValue) {
            this.fixValue = fixValue;
        }
        
        public char getFixValue() {
            return fixValue;
        }
        
        public static ExecType fromFixValue(char value) {
            for (ExecType type : values()) {
                if (type.fixValue == value) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown FIX exec type value: " + value);
        }
    }
    
    /**
     * Execution Restatement Reason enumeration
     */
    public enum ExecRestatementReason {
        GT_RENEWAL(1),                    // GT corporate action
        VERBAL_CHANGE(2),                 // Verbal change
        REPRICING(3),                     // Repricing of order
        BROKER_OPTION(4),                 // Broker option
        PARTIAL_DECLINE(5),               // Partial decline of OrderQty
        CANCEL_ON_DISCONNECT(6),          // Cancel on Trading Halt
        MARKET_OPTION(7),                 // Market (Exchange) option
        CANCELED_NOT_BEST(8),             // Canceled, not best
        WAREHOUSE_RECAP(9),               // Warehouse Recap
        PEG_REFRESH(10),                  // Peg Refresh
        OTHER(99);                        // Other
        
        private final int value;
        
        ExecRestatementReason(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Check if this execution report represents a fill (partial or complete)
     */
    public boolean isFill() {
        return execType == ExecType.TRADE || 
               execType == ExecType.PARTIAL_FILL || 
               execType == ExecType.FILL;
    }
    
    /**
     * Check if the order is complete (fully filled, canceled, rejected, or expired)
     */
    public boolean isOrderComplete() {
        return ordStatus == Order.OrderStatus.FILLED ||
               ordStatus == Order.OrderStatus.CANCELED ||
               ordStatus == Order.OrderStatus.REJECTED ||
               ordStatus == Order.OrderStatus.EXPIRED ||
               ordStatus == Order.OrderStatus.DONE_FOR_DAY;
    }
    
    /**
     * Calculate remaining quantity
     */
    public BigDecimal getRemainingQty() {
        if (orderQty != null && cumQty != null) {
            return orderQty.subtract(cumQty);
        }
        return leavesQty;
    }
    
    /**
     * Get fill percentage
     */
    public BigDecimal getFillPercentage() {
        if (orderQty != null && cumQty != null && orderQty.compareTo(BigDecimal.ZERO) > 0) {
            return cumQty.divide(orderQty, 4, BigDecimal.ROUND_HALF_UP)
                         .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }
}