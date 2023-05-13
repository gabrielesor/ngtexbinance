package it.ngt.trading.exchange.binance.spot;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.spot.Convert;
import com.binance.connector.client.impl.spot.Market;
import com.binance.connector.client.impl.spot.Trade;
import com.binance.connector.client.impl.spot.Wallet;
import com.fasterxml.jackson.core.JsonProcessingException;

import it.ngt.trading.core.entity.Balance;
import it.ngt.trading.core.entity.ChannelType;
import it.ngt.trading.core.entity.ITick;
import it.ngt.trading.core.entity.Order;
import it.ngt.trading.core.entity.OrderType;
import it.ngt.trading.core.entity.Pair;
import it.ngt.trading.core.entity.Price;
import it.ngt.trading.core.entity.Tick;
import it.ngt.trading.core.entity.TraderAction;
import it.ngt.trading.core.entity.TraderActionCode;
import it.ngt.trading.core.entity.WayType;
import it.ngt.trading.core.messages.IMessageType;
import it.ngt.trading.core.util.JsonUtil;
import it.ngt.trading.core.util.MathUtil;
import it.ngt.trading.core.util.TimeUtil;
import it.ngt.trading.exchange.ExchangeAbstract;
import it.ngt.trading.exchange.ExchangeErrorCode;
import it.ngt.trading.exchange.ExchangeException;
import it.ngt.trading.exchange.IExchange;
import it.ngt.trading.exchange.ITickExchange;
import it.ngt.trading.exchange.MarketEnum;
import it.ngt.trading.exchange.binance.spot.beans.BinanceBalance;
import it.ngt.trading.exchange.binance.spot.beans.BinanceConvertOrder;
import it.ngt.trading.exchange.binance.spot.beans.BinanceConvertResponse;
import it.ngt.trading.exchange.binance.spot.beans.BinanceOrder;
import it.ngt.trading.exchange.binance.spot.beans.BinancePrice;
import it.ngt.trading.exchange.binance.spot.beans.BinanceTick;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.ExchangeInfoSpot;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Filter;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Symbol;
import lombok.extern.slf4j.Slf4j;

/**
 * Api documentation:
 * 	https://docs.binance.us/
 * 	https://binance-docs.github.io/apidocs/spot/en/#order-status-user_data
 * 	Orders filters
 * 		https://dev.binance.vision/t/min-notional-filter-error/9746
 * 		https://dev.binance.vision/t/precision-vs-scale-for-quantityprecision/3966/5
 * 		https://mwlang.github.io/binance/Binance/Responses/PriceFilter.html
 * 
 * Binance Convert
 * 	https://www.binance.com/en/blog/otc/3-reasons-why-traders-use-binance-convert-421499824684903125
 * 	https://www.binance.com/en/support/faq/how-to-use-binance-convert-e8c7579382ea403aa4a4a6eec469659d
 * 	https://www.bsc.news/post/everything-you-need-to-know-about-binance-convert
 * 
 * Api Java library
 * 	https://github.com/binance/binance-connector-java
 * 	https://www.javadoc.io/doc/io.github.binance/binance-connector-java/latest/index.html
 *
 * Current informations
 * 	https://www.coingecko.com/en/exchanges/binance
 * 		Currently, there are 357 coins and 1414 trading pairsCurrently,
 * 		there are 357 coins and 1414 trading pairs.
 *  	Binance 24h volume is reported to be at $7,140,493,375.97
 */
@Slf4j
public class BinanceSpotExchange extends ExchangeAbstract implements IExchange {

	private final SpotClientImpl client;

	private final Convert convertClient;
	private final Market marketClient;
	private final Wallet walletClient;
	private final Trade tradeClient;
	
	private final List<Pair> pairs;
	private final Map<String, Pair> pairsMap;

	private List<Price> prices;
	private Map<String, Price> pricesMap;
	
	private static final int DAYS_OFFSET_MAX = 30;	
	
	public BinanceSpotExchange(String accountName, String apiKey, String apiSecret) throws ExchangeException {
		super(accountName);
		client = new SpotClientImpl(apiKey, apiSecret);
		convertClient = client.createConvert();
		marketClient = client.createMarket();
		tradeClient = client.createTrade();
		walletClient = client.createWallet();
	
		pricesMap = this.loadPricesMap();
		pairsMap = this.loadPairsMap();
		//this.updatePrices();
		
		prices = this.loadPrices();
		pairs = this.loadPairs();
		
	}
	
	@Override
	public String getName() {
		return MarketEnum.BN_S.getCode();
	}
	
	@Override
	public String getFriendlyName() {
		return MarketEnum.BN_S.getName();
	}	

	@Override
	public String extractBaseCurrency(String pair) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String extractQuoteCurrency(String pair) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ITickExchange buildTickFromPayload(String payload) throws JsonProcessingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ITickExchange> buildTicksFromPayload(String payload) throws JsonProcessingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ITick toTick(ITickExchange tickExchange) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String buildPingCommand() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String buildSubscribeCommand(int channelId, List<String> pairs, ChannelType channelType)
			throws JsonProcessingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMessageType selectMessageType(String payload, ChannelType channelType) throws JsonProcessingException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Balance getBalance(String currency) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String buildPairName(String baseCurrency, String quoteCurrency) {
		return baseCurrency + quoteCurrency;
	}

	@Override
	public String buildPairAliased(String baseCurrency, String quoteCurrency) {
		return baseCurrency + quoteCurrency;
	}
	
	@Override
	public Map<String, Balance> getBalances() throws ExchangeException {
		
		Map<String, Balance> balancesMap = new TreeMap<>();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "SPOT");	
        parameters.put("needBtcValuation", "true");        
		String result = walletClient.getUserAsset(parameters);
		System.out.println("result:\n" + result);
		if (log.isDebugEnabled()) log.debug("API executed getBalances, binanceResult:\n" + result);
		try {
			/*
				[
				    {
				       "asset":"BTC",
				       "free":"55.27772165",
				       "locked":"0",
				       "freeze":"0.2975",
				       "withdrawing":"0",
				       "ipoable":"0",
				       "btcValuation":"55.57522165"
				    },
				    {
				       "asset":"ETH",
				       "free":"46.37437279",
				       "locked":"0",
				       "freeze":"0",
				       "withdrawing":"0",
				       "ipoable":"0",
				       "btcValuation":"3.10026594"
				    }
				 ]			
			 */
			BinanceBalance[] bbalances = (BinanceBalance[]) JsonUtil.fromJson(result, BinanceBalance[].class);
			for(BinanceBalance bbalance : bbalances) {
				if (log.isDebugEnabled()) log.debug("bbalance: " +  bbalance);
				Balance balance = new Balance();
				balance.setAvailable(Double.valueOf(bbalance.getFree()));
				balance.setCurrency(bbalance.getAsset());
				balance.setLocked(Double.valueOf(bbalance.getLocked()));
				balance.setFreeze(Double.valueOf(bbalance.getFreeze()));
				balance.setValuationAsset("BTC");	//TODO:param
				balance.setValuation(Double.valueOf(bbalance.getBtcValuation()));
				balancesMap.put(balance.getCurrency(), balance);
			}
		} catch (JsonProcessingException e) {
			String message = "exchange error in getBalances, exception: "  + e;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);
		}
		
		return balancesMap;
		
	}

	@Override
	public Order getOrderRaw(String orderId) throws ExchangeException {
		return null;
	}

	/**
		{
		  "symbol": "BTCEUR",
		  "orderId": 2881224314,
		  "orderListId": -1,
		  "clientOrderId": "NzOZ9fWYKjCjPIboJA2rY2",
		  "transactTime": 1682533597834,
		  "price": "25000.00000000",
		  "origQty": "0.00050000",
		  "executedQty": "0.00000000",
		  "cummulativeQuoteQty": "0.00000000",
		  "status": "NEW",
		  "timeInForce": "GTC",
		  "type": "LIMIT",
		  "side": "BUY",
		  "workingTime": 1682533597834,
		  "fills": [],
		  "selfTradePreventionMode": "NONE"
		}
	 */
	@Override
	public String doOrderRaw(TraderAction action) throws ExchangeException {
		
		if (log.isDebugEnabled()) log.debug("doOrderRaw started, action: " + action);
		
		String orderId;
		
		double price = MathUtil.truncateDecimal(action.getPrice(), action.getPriceDecimals());
		double quantity = MathUtil.truncateDecimal(action.getQuantity(), action.getQuantityDecimals());
		String priceS = String.format("%16." + action.getPriceDecimals() + "f", price).trim();
		String quantityS = String.format("%16." + action.getQuantityDecimals() + "f", quantity).trim();
		if (log.isDebugEnabled()) log.debug("priceSx: " + priceS + ", price: " + action.getPrice() + ", priceDecimals: " + action.getPriceDecimals() + ", quantityS: " + quantityS + ", quantity: " + quantity + ", quantityDecimals: " + action.getQuantityDecimals());
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", action.getPair());
        parameters.put("side", this.buildSide(action.getCode()));
        parameters.put("type", this.buildOrderTypeFrom(action));
        parameters.put("quantity", quantityS);  
        parameters.put("price", priceS);
        parameters.put("timeInForce", this.buildTimeInForceFrom(action));
        if (log.isDebugEnabled()) log.debug("before newOrder, parameters: " + parameters);
        String result = client.createTrade().newOrder(parameters);
        if (log.isDebugEnabled()) log.debug("after newOrder, result: " + result);
        try {
			BinanceOrder border = (BinanceOrder) JsonUtil.fromJson(result, BinanceOrder.class);
			if (log.isDebugEnabled()) log.debug("doOrderRaw order executed, order: " + border);
			long borderId = border.getOrderId();
			orderId = borderId + "";
		} catch (JsonProcessingException e) {
			String message = "exchange error in doOrder, action: " + action + ", exception: "  + e;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);
		}
		
		return orderId;
		
	}
	
	@Override
	public List<Order> getOpenOrders(String pair) throws ExchangeException {
		
		List<Order> orders = new ArrayList<>();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        if (pair != null) {
            parameters.put("symbol", pair);        	
        }
        String result = tradeClient.getOpenOrders(parameters);
		if (log.isDebugEnabled()) log.debug("API executed getOpenOrders, binanceResult:\n" + result);
                
        try {
			BinanceOrder[] borders = (BinanceOrder[]) JsonUtil.fromJson(result, BinanceOrder[].class);
			for(BinanceOrder border : borders) {
				if (log.isDebugEnabled()) log.debug("border: " + border);
				Order order = this.buildOrder(border, result);
				orders.add(order);
			}
		} catch (JsonProcessingException e) {
			String message = "exchange error in getOpenOrders, exception: "  + e;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);
		}
		
		return orders;
		
	}
	
	/*
	 	NEW order
		  {
		    "symbol": "DOTBTC",
		    "orderId": 1002599492,
		    "orderListId": -1,
		    "clientOrderId": "web_a5e0dce1b05d4dd9bde82009ec470fd7",
		    "price": "0.00020000",
		    "origQty": "1.00000000",
		    "executedQty": "0.00000000",
		    "cummulativeQuoteQty": "0.00000000",
		    "status": "NEW",
		    "timeInForce": "GTC",
		    "type": "LIMIT",
		    "side": "BUY",
		    "stopPrice": "0.00000000",
		    "icebergQty": "0.00000000",
		    "time": 1680799326118,
		    "updateTime": 1680799326118,
		    "isWorking": true,
		    "workingTime": 1680799326118,
		    "origQuoteOrderQty": "0.00000000",
		    "selfTradePreventionMode": "NONE"
		  }	
			  
		FILLED order
		   {
		      "symbol":"BTCEUR",
		      "orderId":2838503151,
		      "orderListId":-1,
		      "clientOrderId":"web_e69cedaf5a0048b4bb5a42f9d3bd1a20",
		      "price":"27702.00000000",
		      "origQty":"0.00050000",
		      "executedQty":"0.00050000",
		      "cummulativeQuoteQty":"13.85100000",
		      "status":"FILLED",
		      "timeInForce":"GTC",
		      "type":"LIMIT",
		      "side":"SELL",
		      "stopPrice":"0.00000000",
		      "icebergQty":"0.00000000",
		      "time":1681219308015,
		      "updateTime":1681219463847,
		      "isWorking":true,
		      "workingTime":1681219308015,
		      "origQuoteOrderQty":"0.00000000",
		      "selfTradePreventionMode":"NONE"
		   }		
	*/
	private Order buildOrder(BinanceOrder border, String result) {
	
		double orderedPrice;
		double filledPrice;
		double filledQuantity = Double.valueOf(border.getExecutedQty());
		
		OrderType orderType = this.buildOrderTypeTo(border.getType(), result);
		if (orderType.equals(OrderType.MARKET)) {
			orderedPrice = 0;
		} else {
			orderedPrice = Double.valueOf(border.getPrice());
		}
		
		if (filledQuantity == 0) {
			filledPrice = 0;
		} else {
			filledPrice = Double.valueOf(border.getPrice());
		}
		
		Order order = new Order();
		order.setId(border.getOrderId() + "");
		order.setWayType(WayType.SPOT);
		order.setPair(border.getSymbol());
		order.setOrderedPrice(orderedPrice);
		order.setOrderedQuantity(Double.valueOf(border.getOrigQty()));
		order.setFilledQuantity(filledQuantity);
		order.setFilledPrice(filledPrice);
		order.setActionCode(this.buildActionCode(border.getSide(), result));
		order.setClosed(this.buildClosed(border.getStatus(), result));
		order.setOrderType(this.buildOrderTypeTo(border.getType(), result));
		order.setCancelled(this.buildCanceled(border.getStatus(), result));
		order.setStatus(border.getStatus());
		order.setFilledAmount(Double.valueOf(border.getCummulativeQuoteQty()));
		order.setCreateTimeMs(border.getTime());
		order.setUpdateTimeMs(border.getUpdateTime());
		order.setRawFormat(result);
		
		return order;
		
	}

	/*
		[
		   {
		      "symbol":"BTCEUR",
		      "orderId":2837568797,
		      "orderListId":-1,
		      "clientOrderId":"web_e04392ab37ed460eb02ab742c97a1df0",
		      "price":"30000.00000000",
		      "origQty":"0.00050000",
		      "executedQty":"0.00000000",
		      "cummulativeQuoteQty":"0.00000000",
		      "status":"NEW",
		      "timeInForce":"GTC",
		      "type":"LIMIT",
		      "side":"SELL",
		      "stopPrice":"0.00000000",
		      "icebergQty":"0.00000000",
		      "time":1681196372405,
		      "updateTime":1681196372405,
		      "isWorking":true,
		      "workingTime":1681196372405,
		      "origQuoteOrderQty":"0.00000000",
		      "selfTradePreventionMode":"NONE"
		   },
		   {
		      "symbol":"BTCEUR",
		      "orderId":2838503151,
		      "orderListId":-1,
		      "clientOrderId":"web_e69cedaf5a0048b4bb5a42f9d3bd1a20",
		      "price":"27702.00000000",
		      "origQty":"0.00050000",
		      "executedQty":"0.00050000",
		      "cummulativeQuoteQty":"13.85100000",
		      "status":"FILLED",
		      "timeInForce":"GTC",
		      "type":"LIMIT",
		      "side":"SELL",
		      "stopPrice":"0.00000000",
		      "icebergQty":"0.00000000",
		      "time":1681219308015,
		      "updateTime":1681219463847,
		      "isWorking":true,
		      "workingTime":1681219308015,
		      "origQuoteOrderQty":"0.00000000",
		      "selfTradePreventionMode":"NONE"
		   }
		]	 
	 */
	@Override
	public List<Order> getOrders(String pair) throws ExchangeException {
		
		List<Order> orders = new ArrayList<>();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        if (pair == null) {
			String message = "exchange error in getOrders, pair cannot be null";
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(ExchangeErrorCode.PAIR_IS_MANDATORY, "Pair cannot be null in Binance to retrieve the orders");        	
        }
        parameters.put("symbol", pair);
        
        String result;
        try {
            result = tradeClient.getOrders(parameters);
    		if (log.isDebugEnabled()) log.debug("API executed getOrders, binanceResult:\n" + result);        	
        } catch(BinanceClientException e) {
        	if (log.isErrorEnabled()) log.error("ERROR in Binance Exchange getOrders, pair: " + pair + ", exception: " + e);
			throw new ExchangeException(BinanceErrorBuilder.getExchangeErrorCode(e.getErrorCode()), "getOrders Binance Exchange error, pair: " + pair);        	        	
        }            

        try {
			BinanceOrder[] borders = (BinanceOrder[]) JsonUtil.fromJson(result, BinanceOrder[].class);
			for(BinanceOrder border : borders) {
				if (log.isDebugEnabled()) log.debug("border: " + border);
				Order order = this.buildOrder(border, result);
				orders.add(order);
			}
		} catch (JsonProcessingException e) {
			String message = "exchange error in getOpenOrders, exception: "  + e;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);
		}
		
		return orders;
		
	}
	
	
	/**
	 * When a Limit Convert order is created, the stream receives two events.
	 *  E.g., convert 2 EUR to 0.00007692 at the price of 26000 EUR (or 0.00003846 BTC):
	 * 		{"e":"balanceUpdate","E":1681551856495,"a":"EUR","d":"-2.00000000","T":1681551856495}
	 * 		{"e":"outboundAccountPosition","E":1681551856495,"u":1681551856495,"B":[{"a":"EUR","f":"39.25687202","l":"0.00000000"}]}
	 *  	The tradeFlow will:
	 *  	{"quoteId":"1432937291948655521","orderId":1432937291948655521,"orderStatus":"PROCESS","fromAsset":"EUR","fromAmount":"2","toAsset":"BTC","toAmount":"0.00007692","ratio":"0.00003846","inverseRatio":"26000.00000000","createTime":1681551856413}
	 * @param fromTime
	 * @param toTime
	 * @return
	 * @throws ExchangeException
	 */
	public List<BinanceConvertOrder> getOrdersConvert(long fromTime, long toTime) throws ExchangeException {
		
		List<BinanceConvertOrder> borders = new ArrayList<>();
        
		if (fromTime == 0 ||  toTime == 0) {
			String message = "fromTime and toTime cannot be equal at zero, fromTime: " + fromTime + ", toTime: " + toTime;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);			
		}
		long betweenDays = TimeUtil.betweenDays(fromTime, toTime);
		if (log.isDebugEnabled()) log.debug("fromTime: " + fromTime + ", toTime: " + toTime + ", betweenDays: " + betweenDays);
						
		long fromTimeDo = fromTime;
		long toTimeDo;
		long now = Instant.now().toEpochMilli();
		int counterDo = 0;		
		do {
			counterDo++;
			toTimeDo = TimeUtil.addDays(fromTimeDo, DAYS_OFFSET_MAX).toEpochMilli();
			if (log.isDebugEnabled()) log.debug("counterDo: " + counterDo
											  + ", fromTimeDo: " + fromTimeDo + ", toTimeDo: " + toTimeDo + ", now: " + now
											  + ", fromTimeDo: " + Instant.ofEpochMilli(fromTimeDo)
											  + ", toTimeDo: " + Instant.ofEpochMilli(toTimeDo)
											  + ", now: " + Instant.ofEpochMilli(now)
											  );
			List<BinanceConvertOrder> bordersDo = this.getOrdersConvertBetween(fromTimeDo, toTimeDo);	
			if (log.isDebugEnabled()) log.debug("retrieved Convert orders, numberOfOrder: " + bordersDo.size());			
			borders.addAll(bordersDo);
			fromTimeDo = toTimeDo + 1;	//add 1 ms		
		} while(toTimeDo<now);
		
		return borders;
		
	}
	
	private List<BinanceConvertOrder> getOrdersConvertBetween(long fromTime, long toTime) throws ExchangeException {
		
		List<BinanceConvertOrder> borders;
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("startTime", fromTime);
        parameters.put("endTime",   toTime);
        String result = convertClient.tradeFlow(parameters);
        
        BinanceConvertResponse response;
        try {
			response = BinanceConvertResponse.buildFromPayload(result);
		} catch (IOException e) {
			throw new ExchangeException("invalid convert response format, result: " + result + ", exception: " + e);
		}
        
        borders = response.getList();
        
		return borders;
		
	}	

	
	private String buildSide(TraderActionCode code) throws ExchangeException {
		
		String side;
		
		switch(code) {
		case BUY:
			side = "BUY";
			break;
		case SELL:
			side = "SELL";
			break;
		default:
			throw new ExchangeException("invalid action code, actionCode: " + code);			
		}
		return side;
		
	}
	
	private TraderActionCode buildActionCode(String bside, String border) {
		
		TraderActionCode actionCode;
		
		switch(bside) {
		case "BUY":
			actionCode = TraderActionCode.BUY;
			break;
		case "SELL":
			actionCode = TraderActionCode.SELL;
			break;
		default:
			if (log.isWarnEnabled()) log.warn("Side not recognized; set UNDEFINED, binanceSide: " + bside + ", binanceOrder: " + border);
			actionCode = TraderActionCode.UNDEFINED;
		}
		
		return actionCode;
		
	}
	
	private boolean buildClosed(String bstatus, String border) {
	
		boolean closed;
		
		switch(bstatus) {
		case "NEW":
			closed = false;
			break;
		case "PARTIALLY_FILLED":
			closed = true;
			break;
		case "FILLED":
			closed = true;
			break;
		case "CANCELED":
			closed = true;
			break;
		case "PENDING_CANCEL":
			if (log.isWarnEnabled()) log.warn("Closed not recognized for status unused; set false, binanceStatus: " + bstatus + ", binanceOrder: " + border);
			closed = false;
			break;
		case "REJECTED":
			closed = true;
			break;
		case "EXPIRED":
			closed = true;
			break;
		case "EXPIRED_IN_MATCH":
			closed = true;
			break;
		default:
			if (log.isWarnEnabled()) log.warn("Closed not recognized; set false, binanceStatus: " + bstatus + ", binanceOrder: " + border);
			closed = false;
		}
		
		return closed;
	}
	
	private boolean buildCanceled(String bstatus, String border) {
	
		boolean canceled;
		
		switch(bstatus) {
		case "NEW":
			canceled = false;
			break;
		case "PARTIALLY_FILLED":
			canceled = false;
			break;
		case "FILLED":
			canceled = false;
			break;
		case "CANCELED":
			canceled = true;
			break;
		case "PENDING_CANCEL":
			if (log.isWarnEnabled()) log.warn("Canceled not recognized for status unused; set false, binanceStatus: " + bstatus + ", binanceOrder: " + border);
			canceled = false;
			break;
		case "REJECTED":
			canceled = true;
			break;
		case "EXPIRED":
			canceled = true;
			break;
		case "EXPIRED_IN_MATCH":
			canceled = true;
			break;
		default:
			if (log.isWarnEnabled()) log.warn("Closed not recognized; set false, binanceStatus: " + bstatus + ", binanceOrder: " + border);
			canceled = false;
		}
		
		return canceled;
	}
	
	private String buildOrderTypeFrom(TraderAction action) {
		
		return action.getPrice()==0?"MARKET":"LIMIT";
	}
	
	private String buildTimeInForceFrom(TraderAction action) throws ExchangeException {
		
		String timeInForce;
		switch(action.getTimeInForce()) {
		case GTC:
			timeInForce = "GTC";
			break;
		case IOC:
			timeInForce = "IOC";
			break;
		case DEFAULT:
			timeInForce = "GTC";
			break;
		default:
			throw new ExchangeException("invalid time in force, action: " + action);
		}
		return timeInForce;
		
	}
	
	private OrderType buildOrderTypeTo(String btype, String border) {
		
		OrderType type;
		
		switch(btype) {
		case "LIMIT":
			type = OrderType.LIMIT;
			break;
		case "MARKET":
			type = OrderType.MARKET;
			break;
		default:
			type = OrderType.UNDEFINED;
			if (log.isWarnEnabled()) log.warn("OrderType not recognized; set UNDEFINED, binanceType: " + btype + ", binanceOrder: " + border);
		}
		return type;
	}

	@Override
	public List<Pair> getPairs() throws ExchangeException {
		return this.pairs;
	}
	
	//key=pairName
	@Override
	public Map<String, Pair> getPairsMap() throws ExchangeException {
		return this.pairsMap;
	}
	//key=pairCode
	@Override
	public Map<String, Pair> getPairsCodeMap() throws ExchangeException {
		return this.pairsMap;
	}		
	
	private List<Pair> loadPairs() {
		
		final List<Pair> list = new ArrayList<>();

		this.pairsMap.forEach((pairName, pair) -> {
			list.add(pair);
		});
		
		return list;
		
	}
	
	private Map<String, Pair> loadPairsMap() throws ExchangeException {

		if (log.isDebugEnabled()) log.debug("loading the pairs");
		
		Map<String, Pair> map = new TreeMap<>();
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		String result = marketClient.exchangeInfo(parameters);
		try {
			ExchangeInfoSpot ei = (ExchangeInfoSpot) JsonUtil.fromJson(result, ExchangeInfoSpot.class);
			
			List<Symbol> symbols = ei.getSymbols();
			if (log.isDebugEnabled()) log.debug("retrievied the symbols, numberOfSymbols: " + symbols.size());
			
			for(Symbol symbol : symbols) {
				
				Float minQuantity = null;
				Float minQuote = null;
				Integer priceDecimals = null;
				Integer quantityDecimals = null;
				List<Filter> filters = symbol.getFilters();
				for(Filter filter : filters) {
					switch(filter.getFilterType()) {
					case "NOTIONAL":
						Price price = pricesMap.get(symbol.getSymbol());
						if (price != null) {
							Float minNotationalDouble = Float.valueOf(filter.getMinNotional());
							Float priceDouble = Float.valueOf(price.getPrice());
							minQuantity = minNotationalDouble / priceDouble;
							minQuote = minNotationalDouble;
							if (log.isTraceEnabled()) log.trace("symbolName: " + symbol.getSymbol() + ", minNotationalDouble: " + minNotationalDouble
																+ ", priceDouble: " + priceDouble + ", minQuantity: " + minQuantity + ", minQuote: " + minQuote);
						} else {
							if (log.isWarnEnabled()) log.warn("pair not found in Prices; minQuantity set to 1, pair: " + symbol.getSymbol());
							minQuantity = -1.0f;
						}
						break;
					case "PRICE_FILTER":
						priceDecimals = this.computeNumberOfDecimals(filter.getTickSize(), symbol);
						break;
					case "LOT_SIZE":
						quantityDecimals = this.computeNumberOfDecimals(filter.getMinQty(), symbol);
						break;
					}
				}
				if (minQuantity == null) {
					throw new ExchangeException("missing NOTIONAL for minQuantity in getPairs, symbol: " + symbol);
				}
				if (minQuote == null) {
					throw new ExchangeException("missing NOTIONAL for minQuote in getPairs, symbol: " + symbol);
				}
				if (priceDecimals == null) {
					throw new ExchangeException("missing PRICE_FILTER for priceDecimals in getPairs, symbol: " + symbol);
				}				
				if (quantityDecimals == null) {
					throw new ExchangeException("missing LOT_SIZE for quantityDecimals in getPairs, symbol: " + symbol);
				}
				BigDecimal minQuantityBig = BigDecimal.valueOf(minQuantity).setScale(quantityDecimals, RoundingMode.CEILING);
				minQuantity = minQuantityBig.floatValue();
				
				
				
				Pair pair = new Pair();
				pair.setName(symbol.getSymbol());
				pair.setCode(symbol.getSymbol());
				pair.setSymbol(symbol.getSymbol());
				pair.setBase(symbol.getBaseAsset());
				pair.setFeeMaker(0);	//TODO:bseo
				pair.setFeeTaker(0);	//TODO:bseo
				pair.setName(symbol.getSymbol());
				pair.setPriceDecimals(priceDecimals);
				pair.setQuantityDecimals(quantityDecimals);
				pair.setBaseMin(minQuantity);
				pair.setQuoteMin(minQuote);
				pair.setQuote(symbol.getQuoteAsset());
				map.put(pair.getName(), pair);
			}
			
		} catch (JsonProcessingException e) {
			throw new ExchangeException("invalid response in getPairs exception: " + e);
		}
		
		if (log.isDebugEnabled()) log.debug("retrieved the pairs, numberOfPairs: " + map.size());
		
		return map;
		
	}
	
	//	10		->  -1
	//	1		->	0	10000
	//	0.1		->	1	1000
	//	0.01	->	2	100
	//	0.0001	->	4	1
	private int computeNumberOfDecimals(String value, Symbol symbol) throws ExchangeException {
		
		int nod;
		
		switch(value) {
		case "10.00000000":
			nod = -1;
			break;
		case "1.0":
			nod = 0;
			break;
		case "1.00":
			nod = 0;
			break;
		case "1.00000000":
			nod = 0;
			break;
		case "0.10":
			nod = 1;
			break;
		case "0.10000000":
			nod = 1;
			break;
		case "0.01":
			nod = 2;
			break;			
		case "0.01000000":
			nod = 2;
			break;			
		case "0.00100000":
			nod = 3;
			break;
		case "0.00010000":
			nod = 4;
			break;			
		case "0.00001000":
			nod = 5;
			break;			
		case "0.00000100":
			nod = 6;
			break;			
		case "0.00000010":
			nod = 7;
			break;
		case "0.00000001":
			nod = 8;
			break;			
		default:
			throw new ExchangeException("stepSize invalid, stepSize: " + value + ", symbol: " + symbol);			
		}
		
		return nod;
		
	}
	
	/**
	 	https://dev.binance.vision/t/min-notional-filter-error/9746
			The value shown in MIN_NOTIONAL is the minimum value you can purchased for a symbol.
			For example, if MIN_NOTIONAL is 10, the order must be at least of 10 USDT.
	
			LOT_SIZE refers to the quantity of your order.
			You must obey the stepSize shown in the filter.
			If stepSize = 0.01, this means that your quantity cannot exceed 2 decimal places.
	 */
	protected List<Symbol> getSymbols() throws ExchangeException {
		
		if (log.isDebugEnabled()) log.debug("retrieving the symbols");
		
		List<Symbol> symbols;
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		String result = marketClient.exchangeInfo(parameters);
		try {
			ExchangeInfoSpot ei = (ExchangeInfoSpot) JsonUtil.fromJson(result, ExchangeInfoSpot.class);
			
			symbols = ei.getSymbols();
			
		} catch (JsonProcessingException e) {
			throw new ExchangeException("invalid response in getPairs exception: " + e);
		}
		
		if (log.isDebugEnabled()) log.debug("retrieved the symbols, numberOfSymbols: " + symbols.size());
		
		return symbols;
	
	}
	
	@Override
	public List<Price> getPrices() throws ExchangeException {
		return this.prices;
	}
	
	@Override
	public Map<String, Price> getPricesMap() throws ExchangeException {
		return this.pricesMap;
	}

	@Override
	public Map<String, Price> getPricesCodeMap() throws ExchangeException {
		return this.pricesMap;
	}
	
	private List<Price> loadPrices() {
		
		final List<Price> list = new ArrayList<>();

		this.pricesMap.forEach((priceName, price) -> {
			list.add(price);
		});
		
		return list;
		
	}
	@Override
	public void refreshPrices() throws ExchangeException {
		
		pricesMap = this.loadPricesMap();
		this.updatePrices();
		
	}	
	
	private void updatePrices() throws ExchangeException {
		
		this.pricesMap.forEach((pairCode, price)-> {	
			Pair pair = this.pairsMap.get(pairCode);
			if (pair == null) {
				if (log.isWarnEnabled()) log.warn("pair not found in updatePrice, pairCode: " + pairCode + ", pair: " + pair);
			} else {
				price.setPairName(pair.getName());				
			}
			
		});
		
	}
		
	private Map<String, Price> loadPricesMap() throws ExchangeException {

		if (log.isDebugEnabled()) log.debug("loading the prices");
		
		Map<String, Price> prices = new TreeMap<>();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		String result = marketClient.tickerSymbol(parameters);
		try {
			BinancePrice[] bprices = (BinancePrice[]) JsonUtil.fromJson(result, BinancePrice[].class);
			if (log.isDebugEnabled()) log.debug("numberOfPrices: " + bprices.length);
			for(BinancePrice bprice : bprices) {
				Price price = new Price();
				price.setPairCode(bprice.getSymbol());
				price.setPairName(bprice.getSymbol());
				price.setPrice(bprice.getPrice());
				
				prices.put(price.getPairName(), price);
			}
		} catch (JsonProcessingException e) {
			throw new ExchangeException("invalid response in getPairs exception: " + e);
		}
		
		if (log.isDebugEnabled()) log.debug("retrieved the prices, numberOfPrices: " + prices.size());
		
		return prices;
	
	}
	
	@Override
	public ITick getCurrentTick(String pair) throws ExchangeException {
		
		if (log.isDebugEnabled()) log.debug("getCurrentTick started, pair: " + pair);
		Tick tick = new Tick();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", pair);
		String result = this.marketClient.ticker(parameters);
		try {
			BinanceTick btick = (BinanceTick) JsonUtil.fromJson(result, BinanceTick.class);
			float price = Float.valueOf(btick.getLastPrice());
			tick.setAsset(btick.getSymbol());
			tick.setAskPrice(price);
			tick.setBidPrice(price);
			tick.setOpenPriceToday(Float.valueOf(btick.getOpenPrice()));
		} catch (JsonProcessingException e) {
			throw new ExchangeException("invalid response in getCurrentTick, pair: " + pair + ", exception: " + e);
		}
		
		return tick;
		
	}
	
	@Override
	public boolean isConvertOrdersManaged() {
		return true;
	}
	
	@Override
	public Pair getPair(String baseCurrency, String quoteCurrency) throws ExchangeException {
		
		String pairName = baseCurrency + quoteCurrency;
		return this.getPairsMap().get(pairName);
		
	}
	
	@Override
	public boolean isReferenceOfExchange(String reference) {
		return reference != null && reference.startsWith("web_");
	}
	
}
