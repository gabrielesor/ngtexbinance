package it.ngt.trading.exchange.binance.spot.simulated;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.Instant;

import it.ngt.trading.core.ProblemException;
import it.ngt.trading.core.entity.Order;
import it.ngt.trading.core.entity.OrderStatus;
import it.ngt.trading.core.entity.OrderType;
import it.ngt.trading.core.entity.Reference;
import it.ngt.trading.core.entity.TraderAction;
import it.ngt.trading.core.entity.TraderActionCode;
import it.ngt.trading.core.entity.TraderWaiting;
import it.ngt.trading.core.entity.WayType;
import it.ngt.trading.core.exchange.ExchangeException;
import it.ngt.trading.core.util.JsonUtil;
import it.ngt.trading.exchange.binance.spot.BinanceSpotExchange;

/**
 * A simulated version of {@link BinanceExchange} that emulates the behavior of placing and retrieving orders
 * without actually interacting with the real Binance Exchange. This class is designed for internal testing purposes
 * and does not perform any real monetary transactions.
 * 
 * <p>This class extends {@link BinanceExchange} and overrides the {@link #doOrder(TraderAction)} and
 * {@link #getOrder(String, String, TraderWaiting)} methods to simulate order management. Instead of sending orders
 * to the real Binance Exchange, orders are stored in an internal map and can be retrieved as if they were executed.
 * 
 * <p>All other functionality (e.g., tick data, pair information, candle API, account balances) is inherited from
 * {@link BinanceExchange}, ensuring that the simulated environment closely mirrors the real Binance Exchange
 * for testing purposes.
 * 
 * <p><strong>Note:</strong> This class is not intended for production use. It is strictly for testing and development.
 */
public class SimulatedAlphaBinanceSpotExchange extends BinanceSpotExchange {
    
	public SimulatedAlphaBinanceSpotExchange(String accountName, String apiKey, String apiSecret) throws ExchangeException {
		super(accountName, apiKey, apiSecret);
	}

	/**
     * A map storing simulated orders that are created and managed internally.
     * These orders are not sent to the real Binance Exchange but are used to emulate
     * the behavior of order placement and retrieval for testing purposes.
     * 
     * <p>The key is the order ID, and the value is the corresponding {@link Order} object.
     */
    private Map<String, Order> simulatedOrders = new HashMap<>();

    /**
     * Simulates placing an order on the Binance Exchange without actually sending it to the real Exchange.
     * The order is stored in an internal map and can be retrieved later using {@link #getOrder(String, String, TraderWaiting)}.
     * 
     * <p>This method supports two types of orders:
     * <ul>
     *   <li><strong>MARKET</strong> orders: These are simulated as being <strong>fully filled</strong> at the current price
     *       of the trading pair, as returned by the underlying Exchange class. The price used may be cached by the
     *       Exchange class and not reflect the absolute latest price from the exchange.</li>
     *   <li><strong>LIMIT</strong> orders: These are simulated as being <strong>not filled at all</strong> and remain in a
     *       "NEW" state, waiting for the market price to reach the specified limit price.</li>
     * </ul>
     * 
     * @param action The {@link TraderAction} representing the order to be placed (e.g., buy/sell, price, quantity).
     * @return A unique order ID for the simulated order.
     * @throws ExchangeException If an error occurs during the simulation of the order placement.
     * @throws ProblemException If there is a problem with the order parameters or internal state.
     */
    @Override
    public String doOrder(TraderAction action) throws ExchangeException, ProblemException {
 
        // Simulates the creation of an order without forwarding it to the real Exchange
        String orderId = generateSimulatedOrderId();
        Order simulatedOrder = createSimulatedOrder(action);
        simulatedOrders.put(orderId, simulatedOrder);
        return orderId;
        
    }

    /**
     * Retrieves a simulated order from the internal map based on the provided order ID.
     * This method emulates the behavior of retrieving an order from the Binance Exchange,
     * but instead returns the order stored in the internal map.
     * 
     * @param orderId The ID of the order to retrieve.
     * @param pair The trading pair associated with the order (unused in this simulated implementation).
     * @param waiting The {@link TraderWaiting} strategy to use (unused in this simulated implementation).
     * @return The simulated {@link Order} associated with the provided order ID, or {@code null} if the order does not exist.
     * @throws ExchangeException If an error occurs during the simulation of the order retrieval.
     * @throws ProblemException If there is a problem with the order ID or internal state.
     */
    @Override
    public Order getOrder(String orderId, String pair, TraderWaiting waiting) throws ExchangeException, ProblemException {
        // Returns the simulated order from the internal map
        return simulatedOrders.get(orderId);
    }

    /**
     * Generates a unique order ID for simulated orders.
     * 
     * @return A unique order ID in the format "SIMULATED-[UUID]".
     */
    private String generateSimulatedOrderId() {
        return "SIMULATED-" + UUID.randomUUID().toString();
    }

    /**
     * Creates a simulated {@link Order} based on the provided {@link TraderAction}.
     * 
     * <p>This method determines whether the order is a MARKET or LIMIT order based on the price
     * specified in the {@link TraderAction}:
     * <ul>
     *   <li>If the order is a <strong>MARKET</strong> order (i.e., the price is 0), it is simulated
     *       as being <strong>fully filled</strong> at the current price of the trading pair, as
     *       returned by the underlying Exchange class. Note that the price used may be cached
     *       by the Exchange class and not reflect the absolute latest price from the exchange.</li>
     *   <li>If the order is a <strong>LIMIT</strong> order (i.e., the price is non-zero), it is
     *       simulated as being <strong>not filled at all</strong> and remains in a "NEW" state.</li>
     * </ul>
     * 
     * @param action The {@link TraderAction} representing the order details.
     * @return A simulated {@link Order} with the provided details.
     */
    private Order createSimulatedOrder(TraderAction action) {
        boolean isMarketOrder = action.getPrice() == 0;
        return isMarketOrder ? createSimulatedMarketOrder(action) : createSimulatedLimitOrder(action);
    }

    /**
     * Initializes the common fields of an {@link Order} that are independent of the order type (Market or Limit).
     * 
     * @param order  The {@link Order} to initialize.
     * @param action The {@link TraderAction} representing the order details.
     */
    private void initializeCommonOrderFields(Order order, TraderAction action) {
    	
        order.setWayType(WayType.SPOT);
        order.setPair(action.getPair());
        order.setBaseCurrency(action.getBaseCurrency());
        order.setQuoteCurrency(action.getQuoteCurrency());
        order.setActionCode(action.getCode());
        order.setCancelled(false);
        order.setOrderedPrice(action.getPrice());
        order.setOrderedQuantity(action.getQuantity());
        order.setExchange(this.getName());
        order.setOrigin("E");
        order.setExecutionTime(Instant.now().getMillis());
        order.setReference(new Reference("SIM-" + UUID.randomUUID().toString().substring(0, 16)));
        
    }

    /**
     * Creates a simulated MARKET {@link Order} based on the provided {@link TraderAction}.
     * The order is marked as "FILLED" to simulate a successfully executed order.
     * 
     * @param action The {@link TraderAction} representing the order details.
     * @return A simulated MARKET {@link Order} with the provided details.
     */
    private Order createSimulatedMarketOrder(TraderAction action) {
    	
        Order order = new Order();
        initializeCommonOrderFields(order, action); // Initialize common fields

        boolean isBuyOrder = action.getCode().equals(TraderActionCode.BUY);
        double feePercentDefault = 0.1;

        order.setOrderType(OrderType.MARKET);
        order.setClosed(true);
        order.setStatus(OrderStatus.FILLED);
        order.setStatusExchange("FILLED");
        order.setFilledQuantity(action.getQuantity());
        order.setFilledPrice(this.getCurrentPrice(action.getPair()));
        order.setFilledAmount(action.getQuantity() * action.getPrice());
        order.setClosedTime(Instant.now().getMillis());
        order.setFeeCurrency(isBuyOrder ? action.getBaseCurrency() : action.getQuoteCurrency());
        order.setFeeQuantity(isBuyOrder ? action.getQuantity() * feePercentDefault : action.getQuantity() * action.getPrice() * feePercentDefault);
        order.setRawFormat(JsonUtil.toJson(order));

        return order;
    }
    
    /**
     * Returns the current price for the specified trading pair.
     * 
     * <p>This method retrieves the price from the underlying {@link BinanceSpotExchange} class,
     * which represents the latest price available. However, it is important to note that:
     * <ul>
     *   <li>The price returned may not be the absolute latest price from the exchange,
     *       as it could be cached by the {@link BinanceSpotExchange} class for performance reasons.</li>
     *   <li>This method does not force a refresh of the price data, as doing so could negatively
     *       impact performance, especially in high-frequency trading scenarios.</li>
     * </ul>
     * 
     * <p>If the most up-to-date price is required, consider using a different approach that
     * explicitly refreshes the price data (if supported by the exchange API).
     * 
     * @param pairName The trading pair for which to retrieve the price (e.g., "BTC/USDT").
     * @return The current price of the specified trading pair, as provided by the underlying
     *         {@link BinanceSpotExchange} class.
     */   
    private double getCurrentPrice(String pairName) {
    	return super.getPrice(pairName).getPrice();
    }

    /**
     * Creates a simulated LIMIT {@link Order} based on the provided {@link TraderAction}.
     * The order is marked as "NEW" to simulate a pending order.
     * 
     * <p>This method explicitly initializes all fields of the {@link Order}, even those that
     * would default to zero or null. This is a deliberate design choice to ensure clarity,
     * consistency, and robustness in the code. By setting all fields explicitly, we:
     * <ul>
     *   <li>Make the code more self-documenting and easier to understand.</li>
     *   <li>Ensure that the behavior of the method is not dependent on the default values
     *       of the programming language, which could change in future versions.</li>
     *   <li>Maintain a consistent structure between this method and other similar methods
     *       (e.g., {@link #createSimulatedMarketOrder(TraderAction)}), making it easier to
     *       compare and maintain the code.</li>
     * </ul>
     * 
     * @param action The {@link TraderAction} representing the order details.
     * @return A simulated LIMIT {@link Order} with the provided details.
     */
    private Order createSimulatedLimitOrder(TraderAction action) {
    	
        Order order = new Order();
        initializeCommonOrderFields(order, action); // Initialize common fields

        String feeTokenDefault = "[token]";

        order.setOrderType(OrderType.LIMIT);
        order.setClosed(false);
        order.setStatus(OrderStatus.NEW);
        order.setStatusExchange("NEW");
        order.setFilledQuantity(0);
        order.setFilledPrice(0);
        order.setFilledAmount(0);
        order.setClosedTime(0);
        order.setFeeCurrency(feeTokenDefault);
        order.setFeeQuantity(0);
        order.setRawFormat(JsonUtil.toJson(order));

        return order;
    }
    
}