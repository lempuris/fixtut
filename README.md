# FIX Protocol Demo - Exchange Hub Simulator

## Overview

This is a demonstration project showcasing the implementation of FIX protocol using QuickFIX/J (open-source FIX engine) in a Spring Boot application. It simulates an Exchange Hub system similar to what you'll be working on, providing hands-on experience with FIX protocol concepts, market data distribution, and order management.

**Note**: This demo uses QuickFIX/J for educational purposes. In production, you'll use Onyx FIX Engine which offers better performance and additional features.

## Architecture

The demo implements a dual-role FIX system:

1. **FIX Acceptor (Exchange Simulator)** - Acts as an exchange that receives and processes orders
2. **FIX Initiator (Client)** - Acts as a trading client that sends orders and receives execution reports

## Key Technologies

- **Spring Boot 3.5.4** - Application framework
- **QuickFIX/J 2.3.1** - Open-source FIX protocol implementation (Demo)
- **Onyx FIX Engine** - Production FIX engine (when licensed)
- **Java 22** - Programming language
- **Gradle** - Build tool
- **FIX 4.4** - Financial Information eXchange protocol version

## Onyx FIX Engine vs QuickFIX/J Comparison

| Feature | Onyx FIX Engine | QuickFIX/J (Used in Demo) |
|---------|----------------|---------------------------|
| **License** | Commercial (Paid) | Open Source (Free) |
| **Performance** | Ultra-low latency (<10 microseconds) | Moderate latency (~100-500 microseconds) |
| **Architecture** | Zero-copy, lock-free algorithms | Traditional threading model |
| **Memory Management** | Zero garbage collection | Standard JVM garbage collection |
| **Message Processing** | 5+ million messages/sec | ~50,000-100,000 messages/sec |
| **LMAX Disruptor** | Built-in integration | Manual integration needed |
| **Support** | Professional support with SLA | Community support |
| **Documentation** | Comprehensive with examples | Good open-source docs |
| **API Design** | Fluent, type-safe builders | Standard FIX message objects |
| **Session Management** | Advanced recovery mechanisms | Basic session recovery |
| **Market Data** | Optimized for high-frequency | Standard market data handling |
| **Monitoring** | Built-in performance metrics | Basic monitoring |
| **Certification** | Exchange certified | May need additional testing |
| **Use Case** | Production trading systems | Development, testing, low-volume |
| **Cost** | $10,000+ per year | Free |

### Why This Demo Uses QuickFIX/J

1. **No License Required** - Allows immediate hands-on learning
2. **Same FIX Concepts** - Protocol implementation is identical
3. **Easy Migration** - Code structure similar to Onyx
4. **Learning Focus** - Understand FIX protocol without licensing complexity

### When to Use Onyx in Production

- **High-frequency trading** requiring microsecond latency
- **Exchange connectivity** with certification requirements
- **Mission-critical systems** needing professional support
- **Large message volumes** (millions per second)
- **Zero downtime** requirements with advanced recovery

## FIX Protocol Concepts Demonstrated

### Message Types Implemented

1. **Session Messages**
   - Logon (35=A) - Session establishment
   - Logout (35=5) - Session termination
   - Heartbeat (35=0) - Keep-alive mechanism
   - Test Request (35=1) - Connectivity testing

2. **Application Messages**
   - New Order Single (35=D) - Submit new orders
   - Execution Report (35=8) - Order status updates
   - Order Cancel Request (35=F) - Cancel existing orders
   - Order Cancel/Replace Request (35=G) - Modify orders
   - Market Data Request (35=V) - Request market prices
   - Market Data Snapshot (35=W) - Price updates

### FIX Fields Used

Key FIX tags implemented in this demo:
- Tag 11 (ClOrdID) - Client Order ID
- Tag 37 (OrderID) - Exchange Order ID
- Tag 55 (Symbol) - Trading instrument
- Tag 54 (Side) - Buy/Sell
- Tag 38 (OrderQty) - Order quantity
- Tag 44 (Price) - Limit price
- Tag 40 (OrdType) - Order type (Market/Limit/Stop)
- Tag 39 (OrdStatus) - Order status
- Tag 150 (ExecType) - Execution type
- Tag 6 (AvgPx) - Average execution price
- Tag 14 (CumQty) - Cumulative filled quantity

## Project Structure

```
src/main/java/com/fxhub/onyxs/
├── config/
│   └── FixEngineConfig.java           # FIX engine configuration
├── fix/
│   ├── acceptor/
│   │   └── FixAcceptor.java          # Exchange simulator component
│   ├── initiator/
│   │   └── FixInitiator.java         # Client component
│   └── handlers/
│       └── AcceptorMessageHandler.java # Message processing logic
├── models/
│   ├── Order.java                    # Order model with FIX enums
│   ├── MarketDataQuote.java          # Market data model
│   └── ExecutionReport.java          # Execution report model
├── services/
│   └── OrderService.java             # Order management service
├── controllers/
│   ├── OrderController.java          # REST API for orders
│   └── FixConnectionController.java  # REST API for FIX connections
└── OnyxsApplication.java              # Spring Boot main class

src/main/resources/
├── application.yml                   # Spring configuration
├── fix-acceptor.properties          # FIX acceptor settings
└── fix-initiator.properties         # FIX initiator settings
```

## Prerequisites

1. **Java 22** - Ensure Java 22 is installed (or Java 21+)
2. **No License Required** - This demo uses QuickFIX/J which is open source
   - For production with Onyx FIX Engine, you'll need a license from https://www.onixs.biz

## Setup Instructions

### 1. Clone and Build

```bash
# Clone the repository
cd D:\Repos\onyxs

# Build the project
./gradlew build

# Or on Windows
gradlew.bat build
```

### 2. Run the Application

```bash
# Run with Gradle
./gradlew bootRun

# Or build and run JAR
./gradlew build
java -jar build/libs/onyxs-0.0.1-SNAPSHOT.jar
```

The application will start on port 8080.

## Testing the Application

### Using Postman Collection

A comprehensive Postman collection is included for easy API testing:

1. **Import the Collection**
   - Open Postman
   - Click "Import" → Select `FIX-Demo-API.postman_collection.json`
   - Also import `FIX-Demo.postman_environment.json` for environment variables

2. **Collection Structure**
   - **FIX Connection Management** - Health checks and connection control
   - **Order Management** - Submit, view, and cancel orders
   - **Test Data** - Quick sample data generation
   - **FX Trading Examples** - Real-world FX trading scenarios
   - **Gold Trading Examples** - XAU/USD trading examples

3. **Features**
   - Pre-configured requests for all endpoints
   - Automatic ClOrdID extraction and storage
   - Response time validation
   - Environment variables for easy configuration

### Manual Testing with cURL

#### 1. Check FIX Connection Status

```bash
# Get status of FIX connections
curl http://localhost:8080/api/fix/status
```

#### 2. Connect FIX Initiator

```bash
# Connect the client to the exchange
curl -X POST http://localhost:8080/api/fix/initiator/connect
```

#### 3. Submit Orders

##### Market Order Example
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "EUR/USD",
    "side": "BUY",
    "orderType": "MARKET",
    "quantity": 100000,
    "currency": "USD"
  }'
```

##### Limit Order Example
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "GBP/USD",
    "side": "SELL",
    "orderType": "LIMIT",
    "quantity": 50000,
    "price": 1.2650,
    "timeInForce": "GTC",
    "currency": "USD"
  }'
```

#### 4. View Orders

```bash
# Get all orders
curl http://localhost:8080/api/orders

# Get active orders
curl http://localhost:8080/api/orders/active

# Get order statistics
curl http://localhost:8080/api/orders/statistics
```

#### 5. Submit Sample Orders

```bash
# Submit pre-configured sample orders
curl -X POST http://localhost:8080/api/orders/test/submit-samples
```

## Understanding the Code

### FIX Acceptor (Exchange Simulator)

The `FixAcceptor` class simulates an exchange:
- Listens on port 9876 for FIX connections
- Processes incoming orders
- Sends execution reports back to clients
- Simulates immediate fills for market orders

### FIX Initiator (Client)

The `FixInitiator` class acts as a trading client:
- Connects to the exchange
- Sends orders via FIX protocol
- Receives and processes execution reports
- Handles automatic reconnection

### Message Flow

1. **Order Submission**
   - Client creates NewOrderSingle (35=D) message
   - Sends to exchange via FIX
   - Exchange processes and stores order

2. **Order Acknowledgment**
   - Exchange sends ExecutionReport (35=8) with ExecType=New
   - Client receives confirmation

3. **Order Fill**
   - For market orders, exchange simulates immediate fill
   - Sends ExecutionReport with ExecType=Trade
   - Updates order status to Filled

### Key Learning Points

1. **Session Management**
   - FIX sessions maintain persistent connections
   - Heartbeats keep connections alive
   - Sequence numbers ensure message integrity

2. **Message Structure**
   - All FIX messages have standard header/trailer
   - Body contains business data
   - Fields are tag=value pairs separated by SOH

3. **Order Lifecycle**
   - Orders progress through various states
   - Each state change triggers an execution report
   - Client tracks orders via ClOrdID

## Monitoring and Logs

### FIX Message Logs

FIX messages are logged to:
- Acceptor: `./fix-logs/acceptor/`
- Initiator: `./fix-logs/initiator/`

### Application Logs

Application logs are written to:
- Console output
- `logs/onyxs-fix-demo.log`

### Health Check

```bash
curl http://localhost:8080/api/fix/health
```

## Common Issues and Solutions

### 1. License File Not Found
**Error**: "License file not found"
**Solution**: Ensure `onyx-license.xml` is in the project root

### 2. Port Already in Use
**Error**: "Address already in use: 9876"
**Solution**: Change the port in `application.yml`

### 3. Connection Refused
**Error**: "Connection refused"
**Solution**: Ensure the acceptor is running before connecting initiator

## Next Steps for Learning

1. **Explore Message Types**
   - Implement OrderStatusRequest (35=H)
   - Add QuoteRequest/Quote functionality
   - Implement OrderMassStatusRequest

2. **Advanced Features**
   - Add FIX session recovery
   - Implement message persistence
   - Add support for multiple sessions

3. **Performance Optimization**
   - Use LMAX Disruptor for order processing
   - Implement message batching
   - Add connection pooling

## Integration with Real Exchange Hub

This demo provides foundation knowledge for:
- Understanding FIX protocol mechanics
- Working with Onyx FIX Engine API
- Building order management systems
- Handling market data distribution

When working on the actual Exchange Hub, you'll extend these concepts to:
- Connect to SmartTrade for FX/Gold trading
- Integrate Refinitiv market data feeds
- Implement hedging operations
- Scale for production performance

## Resources

- **FIX Protocol Specification**: https://www.fixtrading.org/standards/
- **Onyx Documentation**: https://www.onixs.biz/fix-engine-java.html
- **Spring Boot Reference**: https://spring.io/projects/spring-boot
- **FIX Dictionary**: https://www.onixs.biz/fix-dictionary.html

## Support

For questions about:
- **FIX Protocol**: Refer to FIXimate (https://fiximate.fixtrading.org/)
- **Onyx FIX Engine**: Contact Onyx support
- **This Demo**: Review code comments and documentation

## License

This is a demonstration project for educational purposes. Ensure you have proper licensing for production use of Onyx FIX Engine.