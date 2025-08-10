package com.fxhub.onyxs.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Market Data Quote model representing FIX Quote message (35=S) 
 * and Market Data Snapshot (35=W)
 * Used for real-time price updates from market data providers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataQuote {
    
    // Quote identification
    private String quoteID;           // Quote ID (tag 117) - unique quote identifier
    private String quoteReqID;        // Quote Request ID (tag 131) - if quote was requested
    
    // Instrument details
    private String symbol;            // Symbol (tag 55) - e.g., EUR/USD, XAU/USD
    private String securityType;      // Security Type (tag 167) - FOR, CMDTY
    private String securityExchange;  // Security Exchange (tag 207) - market/venue
    
    // Bid side
    private BigDecimal bidPx;         // Bid Price (tag 132)
    private BigDecimal bidSize;       // Bid Size (tag 134) - quantity available at bid
    private String bidSpotRate;       // Bid Spot Rate (tag 188) - for FX
    
    // Ask/Offer side
    private BigDecimal offerPx;       // Offer Price (tag 133)
    private BigDecimal offerSize;     // Offer Size (tag 135) - quantity available at offer
    private String offerSpotRate;     // Offer Spot Rate (tag 189) - for FX
    
    // Mid market
    private BigDecimal midPx;         // Mid Price - calculated as (bid + ask) / 2
    private BigDecimal spread;        // Spread - calculated as ask - bid
    
    // Quote details
    private QuoteType quoteType;      // Indicative, Tradeable, etc.
    private LocalDateTime quoteTime;  // Quote timestamp (tag 52)
    private LocalDateTime validUntilTime; // Valid Until Time (tag 62)
    
    // Market conditions
    private String quoteCurrency;     // Currency (tag 15)
    private BigDecimal minQty;        // Minimum trade quantity
    private BigDecimal maxQty;        // Maximum trade quantity
    
    // Status
    private QuoteStatus status;       // Quote status
    private String quoteCondition;    // Quote Condition (tag 276) - market condition codes
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Quote Type enumeration
     */
    public enum QuoteType {
        INDICATIVE(0),     // Indicative quote (non-tradeable)
        TRADEABLE(1),      // Tradeable quote
        RESTRICTED(2),     // Restricted tradeable quote
        COUNTER(3);        // Counter quote
        
        private final int value;
        
        QuoteType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Quote Status enumeration
     */
    public enum QuoteStatus {
        ACCEPTED(0),       // Quote accepted
        CANCELED(1),       // Quote canceled
        DECLINED(2),       // Quote declined
        EXPIRED(3),        // Quote expired
        QUOTE(4),          // Active quote
        LOCKED(5),         // Quote locked (market closed)
        PASS(6),           // Pass (no quote)
        PENDING(7);        // Pending quote
        
        private final int value;
        
        QuoteStatus(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Calculate mid price from bid and ask
     */
    public void calculateMidPrice() {
        if (bidPx != null && offerPx != null) {
            this.midPx = bidPx.add(offerPx).divide(BigDecimal.valueOf(2));
        }
    }
    
    /**
     * Calculate spread from bid and ask
     */
    public void calculateSpread() {
        if (bidPx != null && offerPx != null) {
            this.spread = offerPx.subtract(bidPx);
        }
    }
    
    /**
     * Check if quote is valid (not expired)
     */
    public boolean isValid() {
        if (validUntilTime == null) {
            return true;
        }
        return LocalDateTime.now().isBefore(validUntilTime);
    }
    
    /**
     * Get spread in basis points (bps)
     * Useful for FX trading analysis
     */
    public BigDecimal getSpreadInBps() {
        if (spread == null || midPx == null || midPx.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        // Spread in bps = (spread / mid price) * 10000
        return spread.divide(midPx, 6, BigDecimal.ROUND_HALF_UP)
                     .multiply(BigDecimal.valueOf(10000));
    }
}