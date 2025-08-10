# Postman Testing Guide for FIX Demo API

## Quick Start

### 1. Import Files
1. Open Postman
2. Click **Import** button
3. Select both files:
   - `FIX-Demo-API.postman_collection.json` - The complete API collection
   - `FIX-Demo.postman_environment.json` - Environment variables

### 2. Select Environment
- Click the environment dropdown (top right)
- Select **"FIX Demo Environment"**

### 3. Start the Application
```bash
# In the project directory
./gradlew bootRun
# Or
java -jar build/libs/onyxs-0.0.1-SNAPSHOT.jar
```

## Testing Workflow

### Step 1: Verify System Health
Run these requests first to ensure the system is running:

1. **FIX Connection Management → Health Check**
   - Expected: `{"status": "UP", "service": "FIX Engine"}`

2. **FIX Connection Management → Get FIX Connection Status**
   - Shows acceptor and initiator connection states

### Step 2: Submit Orders

#### Quick Test with Sample Data
1. **Test Data → Submit Sample Orders**
   - Creates 3 different order types automatically
   - EUR/USD Market, GBP/USD Limit, XAU/USD Stop

#### Manual Order Submission
1. **Order Management → Submit Market Order**
   - Immediate execution at current price
   - ClOrdID is automatically saved for subsequent requests

2. **Order Management → Submit Limit Order**
   - Executes when price reaches limit
   - Includes Time in Force options

### Step 3: Monitor Orders
1. **Order Management → Get All Orders**
   - View all orders with current status

2. **Order Management → Get Order Statistics**
   - Summary of orders by status and side

3. **Order Management → Get Active Orders**
   - Only shows pending/active orders

### Step 4: Order Operations
1. **Order Management → Cancel Order**
   - Uses the saved {{clOrdID}} variable
   - Or manually enter a specific ClOrdID

## Collection Features

### Automatic Variable Management
The collection automatically:
- Extracts and saves `clOrdID` from order submissions
- Stores it in the environment for use in subsequent requests
- Validates response times (<1000ms)

### Pre-configured Test Scripts
Each request includes:
- Response time validation
- Status code checking
- Automatic variable extraction

### Organized by Use Case
- **FIX Connection Management** - System health and connections
- **Order Management** - Core order operations
- **Test Data** - Quick testing utilities
- **FX Trading Examples** - Real-world FX scenarios
- **Gold Trading Examples** - Commodity trading examples

## Testing Scenarios

### Scenario 1: Basic Order Flow
1. Health Check
2. Submit Market Order
3. Get Order by ID (uses saved clOrdID)
4. Get Order Statistics

### Scenario 2: Limit Order Management
1. Submit Limit Order (GBP/USD)
2. Get Active Orders
3. Cancel Order
4. Verify cancellation in statistics

### Scenario 3: Multiple Order Types
1. Submit Sample Orders
2. Get Orders by Status (NEW)
3. Get Orders by Status (FILLED)
4. Clear All Orders

### Scenario 4: FX Trading Simulation
1. EUR/USD Buy 1 Lot
2. GBP/USD Sell with Limit
3. USD/JPY Scalping Order
4. Monitor all positions

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `baseUrl` | `http://localhost:8080` | API base URL |
| `clOrdID` | (dynamic) | Last order's Client Order ID |
| `orderID` | (dynamic) | Last order's Exchange Order ID |
| `testSymbol` | `EUR/USD` | Default trading symbol |
| `testQuantity` | `100000` | Default order size (1 lot) |
| `testAccount` | `DEMO-001` | Default trading account |

## Tips for Testing

### 1. Use Runner for Automated Testing
- Select collection → Run
- Choose test sequence
- Set iterations and delays

### 2. Monitor Console for Details
- View → Show Postman Console
- See request/response details
- Track variable changes

### 3. Customize Request Bodies
- Modify JSON payloads
- Test edge cases
- Try invalid inputs for error handling

### 4. Check Response Times
- All requests should respond < 1000ms
- Market orders are fastest
- Statistics may take longer with many orders

## Common Test Cases

### Valid Orders
- ✅ Market order with valid symbol
- ✅ Limit order with price
- ✅ Stop order with stop price
- ✅ Different Time in Force values

### Invalid Orders (Error Testing)
- ❌ Missing required fields
- ❌ Invalid symbol
- ❌ Negative quantity
- ❌ Cancel non-existent order

### Performance Testing
- Submit 100 orders rapidly
- Check response times
- Monitor memory usage
- Verify order statistics accuracy

## Troubleshooting

### Connection Refused
- Ensure application is running
- Check port 8080 is available
- Verify baseUrl in environment

### Order Not Found
- Check clOrdID is correct
- Verify order wasn't already canceled
- Ensure order exists in system

### Slow Response Times
- Check system resources
- Reduce order volume
- Clear old orders with test endpoint

## FIX Protocol Learning

Each order submission demonstrates:
- **Message Type 35=D** - New Order Single
- **Client Order ID** - Unique identifier (tag 11)
- **Order Status** - Lifecycle tracking (tag 39)
- **Execution Reports** - Status updates (35=8)

Use Postman to understand:
- Order state transitions
- FIX field mappings
- Session management
- Error handling

## Next Steps

1. **Explore FIX Messages**
   - Check application logs for FIX messages
   - Monitor `fix-logs/` directory
   - Observe message sequence numbers

2. **Test Edge Cases**
   - Large order quantities
   - Rapid order submission
   - Concurrent operations

3. **Build Custom Workflows**
   - Create your own collection
   - Add custom test scripts
   - Implement trading strategies