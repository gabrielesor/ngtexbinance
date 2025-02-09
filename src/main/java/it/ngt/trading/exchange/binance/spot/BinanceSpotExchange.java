package it.ngt.trading.exchange.binance.spot;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.spot.Convert;
import com.binance.connector.client.impl.spot.Market;
import com.binance.connector.client.impl.spot.Trade;
import com.binance.connector.client.impl.spot.Wallet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import it.ngt.trading.core.ProblemException;
import it.ngt.trading.core.entity.Asset;
import it.ngt.trading.core.entity.Balance;
import it.ngt.trading.core.entity.Candle;
import it.ngt.trading.core.entity.ChannelType;
import it.ngt.trading.core.entity.ExchangeStatus;
import it.ngt.trading.core.entity.ExchangeStatusCode;
import it.ngt.trading.core.entity.ITick;
import it.ngt.trading.core.entity.Order;
import it.ngt.trading.core.entity.OrderStatus;
import it.ngt.trading.core.entity.OrderType;
import it.ngt.trading.core.entity.Pair;
import it.ngt.trading.core.entity.Price;
import it.ngt.trading.core.entity.Reference;
import it.ngt.trading.core.entity.Tick;
import it.ngt.trading.core.entity.TickSideType;
import it.ngt.trading.core.entity.TraderAction;
import it.ngt.trading.core.entity.TraderActionCode;
import it.ngt.trading.core.entity.WayType;
import it.ngt.trading.core.exchange.ExchangeErrorCode;
import it.ngt.trading.core.exchange.ExchangeException;
import it.ngt.trading.core.exchange.IExchange;
import it.ngt.trading.core.exchange.ITickExchange;
import it.ngt.trading.core.exchange.ExchangeCode;
import it.ngt.trading.core.messages.IMessageType;
import it.ngt.trading.core.util.JsonUtil;
import it.ngt.trading.core.util.MathUtil;
import it.ngt.trading.core.util.TimeUtil;
import it.ngt.trading.exchange.ExchangeAbstract;
import it.ngt.trading.exchange.binance.spot.beans.BinanceBalance;
import it.ngt.trading.exchange.binance.spot.beans.BinanceCandle;
import it.ngt.trading.exchange.binance.spot.beans.BinanceConvertOrder;
import it.ngt.trading.exchange.binance.spot.beans.BinanceConvertResponse;
import it.ngt.trading.exchange.binance.spot.beans.BinanceOrder;
import it.ngt.trading.exchange.binance.spot.beans.BinancePrice;
import it.ngt.trading.exchange.binance.spot.beans.BinanceSpotTicker;
import it.ngt.trading.exchange.binance.spot.beans.BinanceSpotTrade;
import it.ngt.trading.exchange.binance.spot.beans.BinanceSpotTradeData;
import it.ngt.trading.exchange.binance.spot.beans.BinanceTick;
import it.ngt.trading.exchange.binance.spot.beans.BinanceTrade;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.ExchangeInfoSpot;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Filter;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Symbol;
import lombok.extern.slf4j.Slf4j;

/**
 * Api documentation:
 * 	https://docs.binance.us/
 * 
 * Orders
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
	
	//private static final double FEE_PERCENT = 0.001;	//0.1%, TODO:bseo:params
	
	//
	// in Binance a Pair has the same Name, Code and Symbol
	//
	
	//
	//	original Map of Pairs, Prices and Assets
	//	
	//key=pairName
	private final Map<String, Pair> pairsMap = new TreeMap<>();

	//key=pairName
	private final Map<String, Price> pricesMap = new TreeMap<>();
	
	//key=assetAltName
	private final Map<String, Asset> assetsMap = new TreeMap<>();
	
	//
	//	derived Maps and Lists of Pairs, Prices and Assets obtained browsing the original List
	//
	
	private final List<Pair> pairs = new ArrayList<>();
	
	private final List<Price> prices = new ArrayList<>();
	
	private final List<Asset> assets = new ArrayList<>();
	
	private static final int DAYS_OFFSET_MAX = 30;	
	
	public BinanceSpotExchange(String accountName, String apiKey, String apiSecret) throws ExchangeException {
		super(accountName);
		client = new SpotClientImpl(apiKey, apiSecret);
		convertClient = client.createConvert();
		marketClient = client.createMarket();
		tradeClient = client.createTrade();
		walletClient = client.createWallet();
		
		this.loadPricesMap();
		this.loadPairsMap();
		super.alignPairsPrices(pairsMap, pricesMap);		
		this.loadAssetsMap();
		super.alignAssets(pairsMap, pricesMap, assetsMap);
		super.updatePricesConversion(pricesMap);
		
		super.checkPairsPricesAssets();
		
		super.refresh();	// update refresh time
		
	}
	
	@Override
	public String getCode() {
		return ExchangeCode.BN_S.getCode();
	}
	
	@Override
	public String getName() {
		return ExchangeCode.BN_S.getAlias();
	}	

	@Override
	public String extractBaseCurrency(String pair) {
		throw new UnsupportedOperationException("extractBaseCurrency UNSUPPORTED");
	}

	@Override
	public String extractQuoteCurrency(String pair) {
		throw new UnsupportedOperationException("extractQuoteCurrency UNSUPPORTED");
	}

	@Override
	public ITickExchange buildTickFromPayload(String payload) throws JsonProcessingException {
		
		BinanceSpotTrade binanceTrade = (BinanceSpotTrade) JsonUtil.fromJsonJackson(payload, BinanceSpotTrade.class);
	
		return binanceTrade;
		
	}

	/**
	 * Ticker works fine, but for now it's not used
	 */
	private ITickExchange buildTickFromPayloadTicker(String payload) throws JsonProcessingException {
		
		BinanceSpotTicker binanceTick = (BinanceSpotTicker) JsonUtil.fromJsonJackson(payload, BinanceSpotTicker.class);
	
		return binanceTick;
		
	}

	@Override
	public List<ITickExchange> buildTicksFromPayload(String payload) throws JsonProcessingException {
		throw new UnsupportedOperationException("buildTicksFromPayload UNSUPPORTED");
	}

	@Override
	public ITick toTick(ITickExchange tickExchange) {
		
		Tick tick = new Tick();
		
		tick.setMarket(this.getCode());
		tick.setSideType(TickSideType.VIEW);
		BinanceSpotTrade bt = (BinanceSpotTrade)tickExchange;
		BinanceSpotTradeData bd = bt.getData();
		
		double price = Double.valueOf(bd.getP());
		double quantity = Double.valueOf(bd.getQ());
		
		try {
			tick.setId(0);
			tick.setMarket(ExchangeCode.BN_S.getCode());			
			tick.setAsset(bd.getS());	
			tick.setPair(bd.getS());
			Pair pair = this.getPair(tick.getPair());
			tick.setBaseCurrency(pair.getBase());
			tick.setQuoteCurrency(pair.getQuote());

			//
			//a	
			//
			tick.setAskPrice(price);
			tick.setAskWholeLotVolume(0);
			tick.setAskLotVolume(quantity);

			//
			//b	
			//
			tick.setBidPrice(price);
			tick.setBidWholeLotVolume(0);
			tick.setBidLotVolume(quantity);

			//
			//c	
			//
			tick.setClosePrice(price);
			tick.setCloseLotVolume(quantity);

			//
			//v	
			//
			tick.setVolumeToday(quantity);
			// TODO:bseo:check all set at 0
			tick.setVolumeLast24Hours(0);

			//
			//p	
			//
			tick.setVolumeWToday(0);
			tick.setVolumeWLast24Hours(0);

			//
			//t	
			//
			tick.setTradesToday(0);
			tick.setTradesLast24Hours(0);

			//
			//l	
			//
			tick.setLowPriceToday(price);
			tick.setLowPriceLast24Hours(0);

			//
			//h	
			//
			tick.setHighPriceToday(price);
			tick.setHighPriceLast24Hours(0);

			//
			//o	
			//
			tick.setOpenPriceToday(price);
			tick.setOpenPriceLast24Hours(0);	
			
			//this.setFirstTickInSession(this.normalizeColumnBoolean(tickColumns[index++]));
			
			tick.setMoment(Instant.now().toEpochMilli());
		} catch(NumberFormatException e) {
			if (log.isWarnEnabled()) log.warn("Tick with invalid numeric fields, exception: " + e);				
		}		
		
	
		tick.setMoment(Instant.now().toEpochMilli());

		return tick;
		
	}

	@Override
	public String buildPingCommand() {
		
		return null;
		
	}

	/**
		wss://stream.binance.com:9443/stream?streams=btcusdt@trade/ethbtc@trade
		{
		  "stream": "ethbtc@trade",
		  "data": {		
			  "e": "trade",         // "Event type": Indicates this is a trade event.
			  "E": 1665678912345,   // "Event time": Timestamp of the event in milliseconds.
			  "s": "ETHBTC",        // "Symbol": The trading pair for this trade (ETH/BTC in this case).
			  "t": 12345678,        // "Trade ID": Unique identifier for this specific trade.
			  "p": "0.05",          // "Price": The price at which this trade was executed.
			  "q": "2.0",           // "Quantity": The amount of the base asset (ETH) traded.
			  "b": 87654321,        // "Buyer order ID": The ID of the buy order that was matched.
			  "a": 87654322,        // "Seller order ID": The ID of the sell order that was matched.
			  "T": 1665678912345,   // "Trade time": Timestamp of the trade execution in milliseconds.
			  "m": true,            // "Is the buyer the market maker": true if the buyer is the maker, false if the seller is.
			  "M": true             // "Ignore": This field is always present but can be ignored.
			}
		}
	 */
	@Override
	public String buildSubscribeCommand(int channelId, List<String> pairs, ChannelType channelType)
			throws JsonProcessingException {
		
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<pairs.size(); i++) {
			String pair = pairs.get(i);
			if (i==0) {
				sb.append("?streams=");
			} else {
				sb.append("/");
			}
			sb.append(pair.toLowerCase() + "@trade");	
		}
		
		return sb.toString();
		
	}
	/*
	{
	   "stream":"btceur@ticker",
	   "data":{
	      "e":"24hrTicker",
	      "E":1724595459892,
	      "s":"BTCEUR",
	      "p":"-136.41000000",
	      "P":"-0.238",
	      "w":"57103.95279063",
	      "x":"57263.40000000",
	      "c":"57132.49000000",
	      "Q":"0.00007000",
	      "b":"57130.42000000",
	      "B":"0.00576000",
	      "a":"57132.49000000",
	      "A":"0.00607000",
	      "o":"57268.90000000",
	      "h":"57599.00000000",
	      "l":"56579.30000000",
	      "v":"107.58053000",
	      "q":"6143273.50631090",
	      "O":1724509059891,
	      "C":1724595459891,
	      "F":136299847,
	      "L":136318457,
	      "n":18611
	   }
	}
	 */
    private static final Pattern PATTERN_TICKER = Pattern.compile(
    		"^\\{\"stream\":\"[^\"]+\",\"data\":\\{.*\\}\\}$"
    );
    
	/**
	 * Ticker works fine but it's not used
	 */
	private String buildSubscribeCommandTicker(int channelId, List<String> pairs, ChannelType channelType)
			throws JsonProcessingException {
		
		// 	streams=<symbol1>@ticker/<symbol2>@ticker
		//	wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/ethusdt@ticker

		StringBuilder sb = new StringBuilder();
		for(int i=0; i<pairs.size(); i++) {
			String pair = pairs.get(i);
			if (i==0) {
				sb.append("?streams=");
			} else {
				sb.append("/");
			}
			sb.append(pair.toLowerCase() + "@ticker");	
		}
		
		return sb.toString();
		
	}
	
	/*
	{
		  "e": "trade",         // "Event type": Indicates this is a trade event.
		  "E": 1665678912345,   // "Event time": Timestamp of the event in milliseconds.
		  "s": "ETHBTC",        // "Symbol": The trading pair for this trade (ETH/BTC in this case).
		  "t": 12345678,        // "Trade ID": Unique identifier for this specific trade.
		  "p": "0.05",          // "Price": The price at which this trade was executed.
		  "q": "2.0",           // "Quantity": The amount of the base asset (ETH) traded.
		  "b": 87654321,        // "Buyer order ID": The ID of the buy order that was matched.
		  "a": 87654322,        // "Seller order ID": The ID of the sell order that was matched.
		  "T": 1665678912345,   // "Trade time": Timestamp of the trade execution in milliseconds.
		  "m": true,            // "Is the buyer the market maker": true if the buyer is the maker, false if the seller is.
		  "M": true             // "Ignore": This field is always present but can be ignored.
	}
	*/
    private static final Pattern PATTERN_TRADE = Pattern.compile(
    		"^\\{\"stream\":.*\"data\":\\{\"e\":\"trade\",.*\\}$"
    );
    
	@Override
	public IMessageType selectMessageType(String payload, ChannelType channelType) throws JsonProcessingException {
		
		IMessageType message = IMessageType.UNDEFINED;

		if (PATTERN_TRADE.matcher(payload).matches()) {
			message = IMessageType.TICK;
		}
		
		return message;
		
	}

	@Override
	public String buildPairName(String baseCurrency, String quoteCurrency) {
		return baseCurrency + quoteCurrency;
	}
	
	@Override
	public Map<String, Balance> getBalancesMap() throws ProblemException {
		
		Map<String, Balance> balancesMap = new TreeMap<>();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "SPOT");	
        parameters.put("needBtcValuation", "true");        
		String result = walletClient.getUserAsset(parameters);
		System.out.println("result:\n" + result);
		if (log.isDebugEnabled()) log.debug("API executed getBalances, binanceResult:\n" + result);

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
		if (bbalances.length > 0) {
			for(BinanceBalance bbalance : bbalances) {
				if (log.isDebugEnabled()) log.debug("bbalance: " +  bbalance);				
				String asset = bbalance.getAsset();
				balancesMap.put(asset, super.createBalance(asset, bbalance.getFree(), bbalance.getLocked(), bbalance.getFreeze()));
			}			
		}
		
		super.removeBalancesEmpty(balancesMap);
		
		return balancesMap;
		
	}
	@Override
	public Order getOrderRaw(String orderId, String pairName) throws ExchangeException, ProblemException {
		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("symbol", pairName);
		parameters.put("orderId", orderId);   
		try {
			String result = tradeClient.getOrder(parameters);
			if (log.isDebugEnabled()) log.debug("result:\n" + result);
			BinanceOrder border = (BinanceOrder) JsonUtil.fromJson(result, BinanceOrder.class);
			System.out.println("binanceOrder: " + border);	
			Order order = this.buildOrder(border, result);
			return order;
		} catch(BinanceClientException ex) {
			String errorResponse = ex.getMessage();
			if (log.isDebugEnabled()) log.debug("GetOrder failed, errorResponse: " + errorResponse);
			if (errorResponse.equals("{\"code\":-2013,\"msg\":\"Order does not exist.\"}")) {
				throw new ExchangeException(ExchangeErrorCode.ORDER_NOT_FOUND, "orderId: " + orderId);        		
			} else if (errorResponse != null && errorResponse.contains("\"code\":-2026")) {

				// {"code":-2026,"msg":"Order was canceled or expired with no executed qty over 90 days ago and has been archived."}

				Order order = new Order();
				order.setOrigin("P"); // P: expired
				order.setExchange(this.getName());
    		return order;
			} else {
					throw ex;        		
			}
		}
				
	}

	/*
	@Override
	public Order getOrderRaw(String orderId, String pairName) throws ExchangeException, ProblemException {
		return this.getOrder(orderId, pairName);
	}
	
	@Override
	public Order getOrder(String orderId, String pairName) throws ExchangeException {
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", pairName);
        parameters.put("orderId", orderId);   
        try {
    		String result = tradeClient.getOrder(parameters);
    		if (log.isDebugEnabled()) log.debug("result:\n" + result);
    		BinanceOrder border = (BinanceOrder) JsonUtil.fromJson(result, BinanceOrder.class);
    		System.out.println("binanceOrder: " + border);	
    		Order order = this.buildOrder(border, result);
    		return order;        	
        } catch(BinanceClientException ex) {
        	if (log.isDebugEnabled()) log.debug("GetOrder failed, errorCode: " + ex.getMessage());
        	throw ex;
        }
		
	}
	*/

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
		

		if (log.isDebugEnabled()) log.debug("sleeping before Order, sleepMs: " + this.getOrderSleepMs() + ", exchange: " + getName());
		super.sleep(getOrderSleepMs());
		if (log.isDebugEnabled()) log.debug("ended sleep Order, sleepMs: " + this.getOrderSleepMs() + ", exchange: " + getName());		
				
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		this.doOrderUpdateParameters(parameters, action);
		
		String orderId;
        
        if (log.isDebugEnabled()) log.debug("before newOrder, parameters: " + parameters);
        String result = this.tradeClient.newOrder(parameters);
        if (log.isDebugEnabled()) log.debug("after newOrder, result: " + result);
        try {
			BinanceOrder border = (BinanceOrder) JsonUtil.fromJson(result, BinanceOrder.class);
			if (log.isDebugEnabled()) log.debug("doOrderRaw order executed, order: " + border);
			long borderId = border.getOrderId();
			orderId = borderId + "";
		} catch (ProblemException e) {
			String message = "exchange error in doOrder, action: " + action + ", exception: "  + e;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);
		}
		
		return orderId;
		
	}
	
	@Override
	public int getOrderSleepMs() {
		return 250;	//TODO:BSEO:params
	}	
	
	@Override
	public String doOrderSimulated(TraderAction action) {
					
		if (log.isDebugEnabled()) log.debug("doOrderRaw started, action: " + action);
		
		String error;
		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		this.doOrderUpdateParameters(parameters, action);
        
        if (log.isDebugEnabled()) log.debug("before newOrder, parameters: " + parameters);
        try {
            String result = this.tradeClient.testNewOrder(parameters);
            if (log.isDebugEnabled()) log.debug("after newOrder, result: " + result);        	
            error = result.equals("{}")?null:result;
        } catch(BinanceClientException e) {
        	error = e.getMessage();
        }
        
        return error;
		
	}
	
	private void doOrderUpdateParameters(LinkedHashMap<String, Object> parameters, TraderAction action) {
			
		double price = MathUtil.roundDownDecimal(action.getPrice(), action.getPriceDecimals());
		double quantity = MathUtil.roundDownDecimal(action.getQuantity(), action.getQuantityDecimals());
		String priceS = String.format("%16." + action.getPriceDecimals() + "f", price).trim();
		String quantityS = String.format("%16." + action.getQuantityDecimals() + "f", quantity).trim();
		if (log.isDebugEnabled()) log.debug("priceSx: " + priceS + ", price: " + action.getPrice() + ", priceDecimals: " + action.getPriceDecimals() + ", quantityS: " + quantityS + ", quantity: " + quantity + ", quantityDecimals: " + action.getQuantityDecimals());
		
        parameters.put("symbol", action.getPair());
        parameters.put("side", this.buildSide(action.getCode()));
        parameters.put("type", this.buildOrderTypeFrom(action));
        parameters.put("quantity", quantityS);  
        if (action.getPrice() != 0) {
            parameters.put("price", priceS);
            parameters.put("timeInForce", this.buildTimeInForceFrom(action));        	
        }
        if (action.getReference() != null) {
	        parameters.put("newClientOrderId", action.getReference().getValue());
        }  		
		
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
		} catch (ProblemException e) {
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
			  
		FILLED LIMIT order
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

		FILLED MARKET order		   
		  {
		    "symbol":"BTCEUR",
		    "orderId":2838537044,
		    "orderListId":-1,
		    "clientOrderId":"nKyBryDhnomnU981ABV9PA",
		    "price":"0.00000000",
		    "origQty":"0.00050000",
		    "executedQty":"0.00050000",
		    "cummulativeQuoteQty":"13.87415000",
		    "status":"FILLED",
		    "timeInForce":"GTC",
		    "type":"MARKET",
		    "side":"SELL",
		    "stopPrice":"0.00000000",
		    "icebergQty":"0.00000000",
		    "time":1681220336570,
		    "updateTime":1681220336570,
		    "isWorking":true,
		    "workingTime":1681220336570,
		    "origQuoteOrderQty":"0.00000000",
		    "selfTradePreventionMode":"NONE"
		  }	   		
	*/
	/**
	 * 	https://dev.binance.vision/t/explaining-origqty-executedqty-and-cummulativequoteqty/218/2
			origQty is the client’s original quantity from the request, which is 1.5.
			executedQty is how many BNB is matched in the order, which is 0.5
			cummulativeQuoteQty is how much Alice paid for buying BNB in USDT, which is $17.5.	
	 */
	/**
	 * Explains how fees are handled by Binance during trading operations.
	 * 
	 * <p>Binance applies fees depending on the trading pair and the user's settings, especially regarding the use of 
	 * Binance Coin (BNB) for fee payments. The default behavior and alternative scenarios are described below:</p>
	 * 
	 * <h2>Default Behavior:</h2>
	 * <p>By default, Binance charges the trading fees in the <strong>asset received</strong> during the transaction. This 
	 * means that the fee will be deducted from the amount of the asset you receive as a result of the trade. For example:</p>
	 * 
	 * <ul>
	 *     <li><strong>Buy order:</strong> For the trading pair <code>BTCEUR</code>, if you place a buy order, the fee 
	 *     will be charged in <strong>BTC</strong> (the asset you are receiving).</li>
	 *     <li><strong>Sell order:</strong> For the same pair, if you place a sell order, the fee will be charged in 
	 *     <strong>EUR</strong> (the asset you are receiving).</li>
	 * </ul>
	 * 
	 * <h2>Alternative Behavior: Using BNB to Pay Fees:</h2>
	 * <p>Binance offers an option to pay trading fees using <strong>BNB (Binance Coin)</strong>. If the user has enabled 
	 * this option and holds a sufficient balance of BNB, the fees will be charged in BNB, regardless of the trading pair. 
	 * This option provides a discount on trading fees, which is one of the reasons users might choose to activate it.</p>
	 * 
	 * <p>If this option is enabled, even if you're trading a pair like <code>BTCUSDT</code> or <code>BTCEUR</code>, the 
	 * fees will be deducted from your BNB balance. For instance:</p>
	 * 
	 * <ul>
	 *     <li><strong>Buy order:</strong> For the trading pair <code>BTCEUR</code>, the fee will be charged in BNB 
	 *     instead of BTC if this option is active.</li>
	 *     <li><strong>Sell order:</strong> Similarly, for a sell order, the fee will be charged in BNB rather than EUR.</li>
	 * </ul>
	 * 
	 * <h2>Example JSON Responses:</h2>
	 * <p>Here are examples of the JSON responses for both cases:</p>
	 * 
	 * <h3>1. Default Case (Fee in received asset):</h3>
	 * <pre>{@code
	 * [
	 *   {
	 *     "symbol": "BTCEUR",
	 *     "id": 123456789,
	 *     "orderId": 987654321,
	 *     "price": "40000.00",
	 *     "qty": "0.001",
	 *     "commission": "0.0000005",  // Fee paid in BTC (received asset)
	 *     "commissionAsset": "BTC",   // Asset used to pay the fee
	 *     "time": 1622659447000,
	 *     "isBuyer": true,
	 *     "isMaker": false,
	 *     "isBestMatch": true
	 *   }
	 * ]
	 * }</pre>
	 * 
	 * <h3>2. Fee Paid in BNB (with discount):</h3>
	 * <pre>{@code
	 * [
	 *   {
	 *     "symbol": "BTCEUR",
	 *     "id": 123456789,
	 *     "orderId": 987654321,
	 *     "price": "40000.00",
	 *     "qty": "0.001",
	 *     "commission": "0.0005",      // Fee paid in BNB
	 *     "commissionAsset": "BNB",    // Asset used to pay the fee
	 *     "time": 1622659447000,
	 *     "isBuyer": true,
	 *     "isMaker": false,
	 *     "isBestMatch": true
	 *   }
	 * ]
	 * }</pre>
	 * 
	 * <h2>Summary:</h2>
	 * <ul>
	 *   <li>By default, fees are charged in the asset you receive (e.g., BTC for a buy order, EUR for a sell order).</li>
	 *   <li>If the option is enabled, fees can be charged in BNB, providing a discount on the standard fees.</li>
	 * </ul>
	 */	
	private Order buildOrder(BinanceOrder border, String result) throws ExchangeException {
		
		Pair pair = this.getPairsSymbolMap().get(border.getSymbol());
		if (pair == null) {
			if (log.isWarnEnabled()) log.warn("Pair not found converting the order, pairSymbol: " + border.getSymbol() + ", border: " + border);
			pair = new Pair();
			pair.setBaseQuoteDefault();
		}

		OrderType orderType = this.buildOrderTypeTo(border.getType(), result);

		double orderedQuantity = Double.valueOf(border.getOrigQty());
		double filledQuantity = Double.valueOf(border.getExecutedQty());
		double filledAmount = Double.valueOf(border.getCummulativeQuoteQty());
		double orderedPrice = Double.valueOf(border.getPrice());
		double filledPrice = filledQuantity == 0 ? 0 : filledAmount / filledQuantity;
		
		OrderStatus orderStatus = this.buildOrderStatus(border.getStatus());
				
		Order order = new Order();
		order.setId(border.getOrderId() + "");
		order.setWayType(WayType.SPOT);
		order.setOrderType(orderType);
		order.setPair(border.getSymbol());
		order.setBaseCurrency(pair.getBase());
		order.setQuoteCurrency(pair.getQuote());
		order.setActionCode(this.buildActionCode(border.getSide(), result));
		order.setClosed(orderStatus.isClosed());
		order.setCancelled(this.buildCanceled(border.getStatus(), result));
		order.setStatus(orderStatus);
		order.setStatusExchange(border.getStatus());
		order.setOrderedPrice(orderedPrice);
		order.setOrderedQuantity(orderedQuantity);
		order.setFilledQuantity(filledQuantity);
		order.setFilledPrice(filledPrice);
		order.setFilledAmount(filledAmount);
		order.setExchange(this.getName());
		order.setOrigin("E");
		order.setExecutionTime(border.getTime());
		order.setClosedTime(border.getUpdateTime());
		order.setRawFormat(result);
		order.setReference(new Reference(border.getClientOrderId()));

		//
		//	fee data
		//
		String feeCurrency = "[token]";
		double feeQuantity = 0;		
		if (order.getStatus().hasPotentialTrades()) {
			List<it.ngt.trading.core.entity.Trade> trades = this.getTrades(order.getId(), order.getPair());
			String firstFeeCurrency = null;
			if (log.isDebugEnabled()) log.debug("Retrieving Trades, orderId: " + order.getId());
			for (it.ngt.trading.core.entity.Trade trade : trades) {
				feeQuantity += trade.getFeeQuantity();
				if (firstFeeCurrency == null) {
					firstFeeCurrency = trade.getFeeToken();
					feeCurrency = firstFeeCurrency;
				} else {
					if (!firstFeeCurrency.equals(trade.getFeeToken())) {
						feeCurrency = "[token]";
						feeQuantity = 0;
						if (log.isWarnEnabled()) log.warn("The Trades of the Order do not have the same token for the fee, orderId: " + order.getId());
						if (log.isDebugEnabled()) log.debug("Trades with different fee tokens, orderId: " + order.getId() + ", trades: " + trades);
						break;
					}
				}
			}
		}
		order.setFeeCurrency(feeCurrency);
		order.setFeeQuantity(feeQuantity);

		//if (order.getFilledQuantity() == 0) {
		//	feeCurrency = "";
		//	feeQuantity = 0;
		//} else {
			/*
			 * 	https://www.binance.com/en/support/faq/what-is-binance-spot-trading-fee-and-how-to-calculate-e85d6e703b874674840122196b89780a
				How are trading fees calculated?
				Trading fees are always charged in the asset you receive. For example, if you buy ETH/USDT, the fee is paid in ETH. If you sell ETH/USDT, the fee is paid in USDT.
				For example:
				You place an order to buy 10 ETH for 3,452.55 USDT each:
				Trading fee = 10 ETH * 0.1% = 0.01 ETH
				Or you place an order to sell 10 ETH for 3,452.55 USDT each:
				Trading fee = (10 ETH * 3,452.55 USDT) * 0.1% = 34.5255 USDT
			 */
			//Pair pair = this.getPairsSymbolMap().get(border.getSymbol());
			/*
			if (order.isBuy()) {
				feeQuantity = order.getFilledQuantity() * FEE_PERCENT;				
				if (pair == null) {
					if (log.isWarnEnabled()) log.warn("pair not found in calculation of of the fee of an order, pairName: " + border.getSymbol() + ", border: " + border);
					feeCurrency = "unknown";
				} else {
					feeCurrency = pair.getBase();
				}
			} else {
				feeQuantity = order.getFilledAmount() * FEE_PERCENT;								
				if (pair == null) {
					if (log.isWarnEnabled()) log.warn("pair not found in calculation of of the fee of an order, pairName: " + border.getSymbol() + ", border: " + border);
					feeCurrency = "unknown";
				} else {
					feeCurrency = pair.getQuote();
				}
			}
		}
		*/
		
		return order;
		
	}
	
	private OrderStatus buildOrderStatus(String binanceStatus) {
		if (binanceStatus == null) {
	        return null;
	    }
		
		switch (binanceStatus) {         
        case "NEW":
        	return OrderStatus.NEW;
		case "PARTIALLY_FILLED":
			return OrderStatus.PARTIALLY_FILLED;
		case "FILLED":
			return OrderStatus.FILLED;
		case "CANCELED":
			return OrderStatus.CANCELED;
		case "PENDING_CANCEL":
			if (log.isWarnEnabled()) log.warn("Closed not recognized for status unused; set 'closed' to false, binanceStatus: " + binanceStatus);
			return OrderStatus.PENDING_CANCEL;
		case "REJECTED":
			return OrderStatus.REJECTED;
		case "EXPIRED":
			return OrderStatus.EXPIRED;
		case "EXPIRED_IN_MATCH":
			return OrderStatus.EXPIRED;
		default:
			String error = "Binance Order with the Status not recognized during the 'closed' set, status: " + binanceStatus;
			if (log.isErrorEnabled()) log.error(error);
			throw new ProblemException(error);
		}
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
		
		return getOrders(pair, 0);
		
	}
	
    /**
     * Retrieves all closed orders for the given trading pair.
     *
     * @param pair the trading pair to retrieve orders for.
     * @param maximumNumberOfOrders the maximum number of orders to retrieve. If 0, retrieves all orders.
     * @return a list of orders.
     * @throws ExchangeException if there is an error retrieving the orders.
     * @throws ProblemException if there is a problem with the data conversion.
     */
	@Override
    public List<Order> getOrders(String pair, int maximumNumberOfOrders) throws ExchangeException, ProblemException {
   
		List<Order> allOrders = new ArrayList<>();
        int fetchedOrders = 0;
        long lastOrderId = 0;

        if (pair == null) {
            String message = "exchange error in getOrders, pair cannot be null";
            if (log.isErrorEnabled()) log.error(message);
            throw new ExchangeException(ExchangeErrorCode.PAIR_IS_MANDATORY, "Pair cannot be null in Binance to retrieve the orders");
        }

        try {
            while (true) {
                // Prepare the request parameters
                LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("symbol", pair);
                parameters.put("limit", 1000); // Maximum limit per request
                if (lastOrderId != 0) {
                    parameters.put("orderId", lastOrderId);
                }

                String result;
                try {
                    result = tradeClient.getOrders(parameters);
                    if (log.isDebugEnabled()) log.debug("API executed getOrders, binanceResult:\n" + result);
                } catch (BinanceClientException e) {
                    if (log.isErrorEnabled()) log.error("ERROR in Binance Exchange getOrders, pair: " + pair + ", exception: " + e);
                    throw new ExchangeException(BinanceErrorBuilder.getExchangeErrorCode(e.getErrorCode()), "getOrders Binance Exchange error, pair: " + pair);
                }

                BinanceOrder[] borders;
                try {
                    borders = (BinanceOrder[]) JsonUtil.fromJson(result, BinanceOrder[].class);
                } catch (Exception e) {
                    String message = "exchange error in getOrders, exception: " + e;
                    if (log.isErrorEnabled()) log.error(message);
                    throw new ExchangeException(message);
                }

                if (borders.length == 0) {
                    break;
                }

                for (BinanceOrder border : borders) {
                    if (log.isDebugEnabled()) log.debug("border: " + border);
                    Order order = this.buildOrder(border, result);
                    allOrders.add(order);
                    lastOrderId = border.getOrderId();
                }

                fetchedOrders += borders.length;

                // Check if we have fetched enough orders
                if (maximumNumberOfOrders > 0 && fetchedOrders >= maximumNumberOfOrders) {
                    break;
                }

                // If the number of orders retrieved is less than the limit, break the loop
                if (borders.length < 1000) {
                    break;
                }
            }
        } catch (Exception e) {
            String message = "exchange error in getOrders, exception: " + e;
            if (log.isErrorEnabled()) log.error(message);
            throw new ExchangeException(message);
        }

        // If a maximum number of orders is specified, trim the list
        if (maximumNumberOfOrders > 0 && allOrders.size() > maximumNumberOfOrders) {
            return allOrders.subList(0, maximumNumberOfOrders);
        }

        return allOrders;
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
	 * @throws ProblemException 
	 */
	public List<BinanceConvertOrder> getOrdersConvert(long fromTime, long toTime) throws ExchangeException, ProblemException {
		
		List<BinanceConvertOrder> borders = new ArrayList<>();
        
		if (fromTime == 0 ||  toTime == 0) {
			String message = "fromTime and toTime cannot be equal at zero, fromTime: " + fromTime + ", toTime: " + toTime;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);			
		}
		long betweenDays = TimeUtil.calculateDaysBetween(fromTime, toTime);
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
	
	/*
	 * order canceled:
		{
		  "symbol": "BTCEUR",
		  "origClientOrderId": "web_01492108254c4267a2786bac0f922ac0",
		  "orderId": 2922746149,
		  "orderListId": -1,
		  "clientOrderId": "unDQEbhT5QHFZTCMhktXhs",
		  "price": "20000.00000000",
		  "origQty": "0.00050000",
		  "executedQty": "0.00000000",
		  "cummulativeQuoteQty": "0.00000000",
		  "status": "CANCELED",
		  "timeInForce": "GTC",
		  "type": "LIMIT",
		  "side": "BUY",
		  "selfTradePreventionMode": "NONE"
		}
		
		order not found:
		{"code":-2011,"msg":"Unknown order sent."}
			
	 */
	@Override
	public boolean cancelOrder(String orderId, String pair) throws ExchangeException {
	
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", pair);
        parameters.put("orderId", orderId);
        String result = this.tradeClient.cancelOrder(parameters);
        if (log.isDebugEnabled()) log.debug("binanceResult: " + result);
        BinanceResponse response = new BinanceResponse(result);
        
        return !response.isError();	//TODO:BSEO:improvement check the "status"
				
	}
	
	private List<BinanceConvertOrder> getOrdersConvertBetween(long fromTime, long toTime) throws ExchangeException, ProblemException {
		
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
	
	/*private boolean buildClosed(String bstatus, String border) {
	
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
			if (log.isWarnEnabled()) log.warn("Closed not recognized for status unused; set 'closed' to false, binanceStatus: " + bstatus + ", binanceOrder: " + border);
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
			String error = "Binance Order with the Status not recognized during the 'closed' set, status: " + bstatus + ", binanceOrder: " + border;
			if (log.isErrorEnabled()) log.error(error);
			throw new ProblemException(error);
		}
		
		return closed;
	}*/
	
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
			if (log.isWarnEnabled()) log.warn("Canceled not recognized for status unused; set 'canceled' to false, binanceStatus: " + bstatus + ", binanceOrder: " + border);
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
			String error = "Binance Order with the Status not recognized during the 'canceled' set, status: " + bstatus + ", binanceOrder: " + border;
			if (log.isErrorEnabled()) log.error(error);
			throw new ProblemException(error);
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
			type = OrderType.OTHER;
			break;
		}
		return type;
	}
	
	//
	// Pair and Prices, methods begin
	//
	
	@Override
	public void refresh() throws ExchangeException {
	
		this.loadPricesMap();
		this.loadPairsMap();
		super.alignPairsPrices(pairsMap, pricesMap);	
		this.loadAssetsMap();
		super.alignAssets(pairsMap, pricesMap, assetsMap);		
		super.updatePricesConversion(pricesMap);
		
		super.checkPairsPricesAssets();
		
		super.refresh();

	}	

	private void loadPairsMap() throws ExchangeException {

		if (log.isDebugEnabled()) log.debug("loading the pairs");
		
		this.pairsMap.clear();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		String result = marketClient.exchangeInfo(parameters);
		try {
			ExchangeInfoSpot ei = (ExchangeInfoSpot) JsonUtil.fromJson(result, ExchangeInfoSpot.class);
			
			List<Symbol> symbols = ei.getSymbols();
			if (log.isDebugEnabled()) log.debug("retrievied the symbols, numberOfSymbols: " + symbols.size());
			
			for(Symbol symbol : symbols) {
				if (!symbol.getStatus().equals("TRADING")) {
					if (log.isTraceEnabled()) log.trace("pair not in TRADING status; it's skipped, symbol: " + symbol);
					continue;
				}
				
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
							Float priceDouble = (float) price.getPrice();
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
				pair.setExchangeName(this.getName());
				pair.setName(symbol.getSymbol());
				pair.setCode(symbol.getSymbol());
				pair.setSymbol(symbol.getSymbol());
				pair.setSymbolInChannel(symbol.getSymbol());
				pair.setBase(symbol.getBaseAsset());
				pair.setFeeMaker(0);	//TODO:bseo
				pair.setFeeTaker(0);	//TODO:bseo
				pair.setName(symbol.getSymbol());
				pair.setPriceDecimals(priceDecimals);
				pair.setQuantityDecimals(quantityDecimals);
				pair.setBaseMin(minQuantity);
				pair.setQuoteMin(minQuote);
				pair.setQuote(symbol.getQuoteAsset());
				this.pairsMap.put(pair.getName(), pair);
			}
			
		} catch (ProblemException e) {
			throw new ExchangeException("invalid response in getPairs exception: " + e);
		}
		
		if (log.isDebugEnabled()) log.debug("retrieved the pairs, numberOfPairs: " + this.pairsMap.size());
		
		//
		// build the Pairs Derived
		//
		this.buildPairsDerived();
		
	}
	
	/**
	 * build all the Maps and Lists of Pairs from the pairsMap
	 */
	@Override
	protected void buildPairsDerived() {
		
		//
		// build the List of Pairs
		//
		this.pairs.clear();
		this.pairsMap.forEach((pairName, pair) -> {
			this.pairs.add(pair);
		});				
		
	}
	
	private void loadPricesMap() throws ExchangeException {
	
		if (log.isDebugEnabled()) log.debug("loading the prices");
		
		this.pricesMap.clear();
		
	    LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		String result = marketClient.tickerSymbol(parameters);
		try {
			BinancePrice[] bprices = (BinancePrice[]) JsonUtil.fromJson(result, BinancePrice[].class);
			if (log.isDebugEnabled()) log.debug("numberOfPrices: " + bprices.length);
			for(BinancePrice bprice : bprices) {
				
				String pairCode = bprice.getSymbol(); 
				
				/*
				Pair pair = this.pairsMap.get(pairCode);
				if (pair == null) {
					// there are some Prices without a Pair currently supported;
					//	it's correct skip them
					if (log.isDebugEnabled()) log.debug("pair not found loading the prices, exchange: " + this.getName() + " ,pairName: " + pairCode);
					continue;
				}
				*/		
				
				Price price = new Price();
				price.setExchangeName(this.getName());
				price.setPairCode(pairCode);
				price.setPairName(pairCode);
				price.setPriceS(bprice.getPrice());
				price.setPriceBD(MathUtil.convertToBD(price.getPriceS()));
				price.setPrice(MathUtil.convertToDouble(price.getPriceS()));
				price.setPriceReversed(1 / price.getPrice());
				this.pricesMap.put(price.getPairName(), price);
			}
		} catch (ProblemException e) {
			throw new ExchangeException("invalid response in getPairs exception: " + e);
		}
		
		if (log.isDebugEnabled()) log.debug("retrieved the prices, numberOfPrices: " + pricesMap.size());		
		
		//
		// build Prices derived
		//
		this.buildPricesDerived();
		
	}
	
	@Override
	protected void buildPricesDerived() {
	
		//
		// Build the List of Prices
		//
		this.prices.clear();
		this.pricesMap.forEach((priceName, price) -> {
			prices.add(price);
		});
	
	}

	//key=pairNname
	@Override
	public Map<String, Pair> getPairsMap() throws ExchangeException {
		
		return this.pairsMap;
		
	}

	//key=pairCode
	@Override
	public Map<String, Pair> getPairsCodeMap() throws ExchangeException {

		return this.pairsMap;
		
	}

	//key=pairSymbol
	@Override
	public Map<String, Pair> getPairsSymbolMap() throws ExchangeException {

		return this.pairsMap;
		
	}	
	
	@Override
	public List<Pair> getPairs() throws ExchangeException {
		
		return this.pairs;
		
	}
	
	@Override
	public List<Price> getPrices() throws ProblemException {
		
		return this.prices;	
		
	}
	
	@Override
	public Map<String, Price> getPricesMap() throws ProblemException {

		return this.pricesMap;
		
	}
	
	@Override
	public Map<String, Price> getPricesCodeMap() throws ProblemException {
		
		return this.pricesMap;
		
	}
	
	//
	// Pair and Prices, methods end
	//
	

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
			
		} catch (ProblemException e) {
			throw new ExchangeException("invalid response in getPairs exception: " + e);
		}
		
		if (log.isDebugEnabled()) log.debug("retrieved the symbols, numberOfSymbols: " + symbols.size());
		
		return symbols;
	
	}
	
	@Override
	public ExchangeStatus getExchangeStatus() {
	
		ExchangeStatus status;
		
		if (log.isDebugEnabled()) log.debug("systemStatus retrieving");
		try {
			String result = walletClient.systemStatus();
			if (log.isDebugEnabled()) log.debug("systemStatus retrieved, result: " + result);
			BinanceSystemStatusResponse statusResponse = (BinanceSystemStatusResponse) JsonUtil.fromJson(result, BinanceSystemStatusResponse.class);
			
			status = new ExchangeStatus();
			status.setCode(statusResponse.getStatus()==0?ExchangeStatusCode.OPERATIVE:ExchangeStatusCode.MAINTENANCE);
			status.setInfo(statusResponse.getMsg());			
		} catch(Throwable t) {
			status = new ExchangeStatus();
			status.setCode(ExchangeStatusCode.INACCESSIBLE);
			status.setInfo("ERROR: " + t.getMessage() + ", " + t.getClass());						
		}
		
		return status;
		
	}
	
	@Override
	public ITick getCurrentTick(String pair) throws ExchangeException {
		
		if (log.isDebugEnabled()) log.debug("getCurrentTick started, pair: " + pair);
		Tick tick = new Tick();
		
		tick.setMoment(Instant.now().toEpochMilli());
		
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
		} catch (ProblemException e) {
			throw new ExchangeException("invalid response in getCurrentTick, pair: " + pair + ", exception: " + e);
		}
		
		return tick;
		
	}
	
	@Override
	public List<Asset> getAssets() throws ExchangeException, ProblemException {
		return this.assets;
	}
	
	private void loadAssetsMap() throws ExchangeException, ProblemException {
		
		this.assetsMap.clear();
		
		Set<String> assetsInPairs = new TreeSet<>();
		for(Pair pair : pairs) {
			assetsInPairs.add(pair.getBase());
			assetsInPairs.add(pair.getQuote());
		}
		if (log.isDebugEnabled()) log.debug("load Assets in Pairs, exchange: " + this.getName() + ", numberOfAssetsInPair: " + assetsInPairs.size());		
		
		
		Set<String> assets = new TreeSet<>();
		for(Pair pair : this.getPairs()) {
			assets.add(pair.getBase());
			assets.add(pair.getQuote());
		}
		List<Asset> assetsList = new ArrayList<>();
		for(String assetE : assets) {
			assetsList.add(new Asset(this.getName(), assetE, assetE));
		}
		
		for(Asset asset : assetsList) {
			String assetName = asset.getName();
			if (!assetsInPairs.contains(assetName)) {
				// there are some Asset without any Pairs currently supported;
				//	it's correct skip them
				if (log.isDebugEnabled()) log.debug("asset not found in the pairs loading the assets, exchange: " + this.getName() +  ", asset: " + assetName);
				continue;
			}
			
			this.assetsMap.put(asset.getName(), asset);
		}
				
		if (log.isDebugEnabled()) log.debug("load Assets in Pairs, numberOfAssets: " + this.assetsMap.size());		
		
		//
		// build Assets derived
		//
		this.buildAssetsDerived();
		
		
	}
	
	@Override
	protected void buildAssetsDerived() {
		
		//
		// Build the List of Assets
		//
		this.assets.clear();
		this.assetsMap.forEach((assetName, asset) -> {
			assets.add(asset);
		});	
		
	}

	@Override
	public boolean isConvertOrdersManaged() {
		return true;
	}
	
	@Override
	public Pair getPair(String baseCurrency, String quoteCurrency) throws ExchangeException {
		
		String pairName = baseCurrency + quoteCurrency;
		return this.getPair(pairName);
		
	}
	
	@Override
	public Pair getPair(String pairName) {
		
		Pair pair = this.pairsMap.get(pairName);
		if (pair == null) {
			throw new ProblemException("Pair not found, pairName: " + pairName);
		}
		return pair;
		
	}
	
	@Override
	public boolean isPairExist(String pairName) {
		return this.pairsMap.containsKey(pairName);
	}

	@Override
	public boolean isReferenceOfExchange(String reference) {
		return reference != null && reference.startsWith("web_");
	}
	
	/**
	 * return true is the the getBalances method returns the fields "locked" set
	 */
	@Override
	public boolean isBalanceLockedManaged() {
		return true;
	}
	
	/**
	 * return true is the the getBalances method returns the fields "locked" set
	 */
	@Override
	public boolean isBalanceFreezeManaged() {
		return true;
	}	
	
	/**
	 * return true is the the getBalances method returns the fields "locked" set
	 */
	@Override
	public boolean isBalanceValuationManaged() {
		return true;
	}
	
	@Override
	public boolean isManageSimulationOrder() {
		return true;
	}
	
	@Override
	public boolean isReferenceMustBeUnique() {
		return true;
	}
	
	@Override
	public List<it.ngt.trading.core.entity.Trade> getTrades(String orderId, String pair) {
		
		List<it.ngt.trading.core.entity.Trade> trades = new ArrayList<>();
		
		LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		if (pair != null && orderId != null) {
			parameters.put("symbol", pair);  
	        parameters.put("orderId", orderId);
		}
        String result = tradeClient.myTrades(parameters);
        if (log.isDebugEnabled()) log.debug("API executed myTrades, tradesResult:\n" + result);
        
        try {
					
			BinanceTrade[] btrades = (BinanceTrade[]) JsonUtil.fromJson(result, BinanceTrade[].class);
			JsonElement jsonElement = (JsonElement) JsonUtil.fromJson(result, JsonElement.class);
			JsonArray tradesJsonArray = jsonElement.getAsJsonArray();

			for (int i = 0; i < btrades.length; i++) {
				BinanceTrade btrade = btrades[i];

				if (log.isDebugEnabled()) {
						log.debug("binance trade: " + btrade);
				}

				it.ngt.trading.core.entity.Trade trade = new it.ngt.trading.core.entity.Trade();

				trade.setTradeId(String.valueOf(btrade.getId()));
				trade.setOrderId(String.valueOf(btrade.getOrderId()));
				trade.setPair(btrade.getSymbol());
				trade.setSide(btrade.isBuyer());
				trade.setQuantityB(Double.parseDouble(btrade.getQty()));
				trade.setQuantityQ(Double.parseDouble(btrade.getQuoteQty()));
				trade.setPriceBQ(Double.parseDouble(btrade.getPrice()));
				trade.setFeeQuantity(Double.parseDouble(btrade.getCommission()));
				trade.setFeeToken(btrade.getCommissionAsset());
				trade.setMaker(btrade.isMaker());
				trade.setOrigin("E");
				trade.setExchange(this.getName());
				trade.setPayload(tradesJsonArray.get(i).toString());
				trade.setExecutionTime(btrade.getTime());

				trades.add(trade);
			}
		} catch (ProblemException e) {
			String message = "exchange error in getTrades, exception: "  + e;
			if (log.isErrorEnabled()) log.error(message);
			throw new ExchangeException(message);
		}
		
		return trades;
	}

	@Override
	public List<String> getTimeFrames() {
		return Arrays.asList("1m", "3m", "5m", "15m", "30m", "1h", "2h", "4h", "6h", "8h", "12h", "1d", "3d",  "1w", "1M");
	}

	/**
		[
		  [
		    1731074400000,
		    "0.03864000",
		    "0.03877000",
		    "0.03858000",
		    "0.03873000",
		    "820.95240000",
		    1731076199999,
		    "31.75209560",
		    4129,
		    "426.80000000",
		    "16.50715200",
		    "0"
		  ],
		  [
		    1731076200000,
		    "0.03873000",
		    "0.03875000",
		    "0.03844000",
		    "0.03846000",
		    "2449.09780000",
		    1731077999999,
		    "94.38465781",
		    8715,
		    "1490.88620000",
		    "57.44565952",
		    "0"
		  ],
		  [
		    1731078000000,
		    "0.03846000",
		    "0.03852000",
		    "0.03833000",
		    "0.03844000",
		    "1041.32280000",
		    1731079799999,
		    "40.01850513",
		    4999,
		    "546.82220000",
		    "21.01454038",
		    "0"
		  ]
		]	 
	 */
	/**
		[
		  [
		    1499040000000,      // Open time
		    "0.01634790",       // Open
		    "0.01700000",       // High
		    "0.01575800",       // Low
		    "0.01577100",       // Close
		    "148976.11427815",  // Volume
		    1499644799999,      // Close time
		    "2434.19055334",    // Quote asset volume
		    308,                // Number of trades
		    "1756.87402397",    // Taker buy base asset volume
		    "28.46694368",      // Taker buy quote asset volume
		    "17928899.62484339" // Ignore
		  ]
		]	 
	*/
	@SuppressWarnings("unchecked")
	@Override
	public List<Candle> getCandles(String pair, int timeframeSec, int limit) {
		
	    // Check if the limit is negative
	    if (limit < 0) {
	        throw new ExchangeException("The limit parameter cannot be negative, limit: " + limit);
	    }
	    
		List<Candle> candles = new ArrayList<>();
		
	    try {

	        // Prepare API request parameters
	        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
	        parameters.put("symbol", pair.toUpperCase());

	        // Convert timeframeSec to Binance interval format
	        String interval = TimeUtil.toTimeUnits(timeframeSec);
	        if (interval.equals(timeframeSec + "")) {
	            throw new IllegalArgumentException("Invalid timeframeSec: " + timeframeSec);
	        }
	        parameters.put("interval", interval);

	        // Optionally, set the limit for the number of candles to fetch
	        if (limit > 0) {
		        parameters.put("limit", limit);	        	
	        }

	        // Call the Binance API to get candlestick data
	        String resultJson = marketClient.klines(parameters);

	        // Parse the JSON response
	        List<List<Object>> klineData = (List<List<Object>>) JsonUtil.fromJson(resultJson, List.class);

            // Iterate over the kline data
            for (List<Object> klineEntry : klineData) {
            	
                BinanceCandle binanceCandle = new BinanceCandle();

                binanceCandle.setOpenTime(((Double)klineEntry.get(0)).longValue());
                binanceCandle.setOpen(klineEntry.get(1).toString());
                binanceCandle.setHigh(klineEntry.get(2).toString());
                binanceCandle.setLow(klineEntry.get(3).toString());
                binanceCandle.setClose(klineEntry.get(4).toString());
                binanceCandle.setVolume(klineEntry.get(5).toString());
                binanceCandle.setCloseTime(((Double)klineEntry.get(6)).longValue());
                binanceCandle.setQuoteAssetVolume(klineEntry.get(7).toString());
                binanceCandle.setNumberOfTrades(((Double)klineEntry.get(8)).intValue());
                binanceCandle.setTakerBuyBaseAssetVolume(klineEntry.get(9).toString());
                binanceCandle.setTakerBuyQuoteAssetVolume(klineEntry.get(10).toString());
                binanceCandle.setIgnore(klineEntry.get(11).toString());

                // Map BinanceCandle to your own Candle object
                Candle candle = new Candle();
                candle.setPair(pair);
                candle.setTimeframe(timeframeSec);
                candle.setTime(binanceCandle.getOpenTime());
                candle.setOpen(Double.parseDouble(binanceCandle.getOpen()));
                candle.setClose(Double.parseDouble(binanceCandle.getClose()));
                candle.setMin(Double.parseDouble(binanceCandle.getLow()));
                candle.setMax(Double.parseDouble(binanceCandle.getHigh()));
                candle.setTimeStart(binanceCandle.getOpenTime());
                candle.setTimeEnd(binanceCandle.getCloseTime());
                // Add the candle to the list
                candles.add(candle);
            }
	    } catch (Exception e) {
	        throw new ExchangeException("Get Candles failed, pair: " + pair + ", timeframeSec: " + timeframeSec + ", limit: " + limit + ", exception: " + e);
	    }
	    return candles;		
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Candle> getCandles(String pair, int timeframeSec, long startTime, long endTime) throws ExchangeException {
		List<Candle> candles = new ArrayList<>();
		
		try {
			// Prepare API request parameters
			LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
			parameters.put("symbol", pair.toUpperCase());
			
			// Convert timeframeSec to Binance interval format 
			String interval = TimeUtil.toTimeUnits(timeframeSec);
			if (interval.equals(timeframeSec + "")) {
				throw new IllegalArgumentException("Invalid timeframeSec: " + timeframeSec);
			}
			parameters.put("interval", interval);
			
			// Add one candle before startTime and one after endTime by adjusting the time range
			long adjustedStartTime = startTime - (timeframeSec * 1000L); // Subtract one timeframe
			long adjustedEndTime = endTime + (timeframeSec * 1000L); // Add one timeframe
			
			// Add adjusted time range parameters
			parameters.put("startTime", adjustedStartTime);
			parameters.put("endTime", adjustedEndTime);
			
			// Call the Binance API to get candlestick data
			String resultJson = marketClient.klines(parameters);
			
			// Parse the JSON response
			List<List<Object>> klineData = (List<List<Object>>) JsonUtil.fromJson(resultJson, List.class);
			
			// Iterate over the kline data
			for (List<Object> klineEntry : klineData) {
				BinanceCandle binanceCandle = new BinanceCandle();
				
				binanceCandle.setOpenTime(((Double)klineEntry.get(0)).longValue());
				binanceCandle.setOpen(klineEntry.get(1).toString());
				binanceCandle.setHigh(klineEntry.get(2).toString());
				binanceCandle.setLow(klineEntry.get(3).toString());
				binanceCandle.setClose(klineEntry.get(4).toString());
				binanceCandle.setVolume(klineEntry.get(5).toString());
				binanceCandle.setCloseTime(((Double)klineEntry.get(6)).longValue());
				binanceCandle.setQuoteAssetVolume(klineEntry.get(7).toString());
				binanceCandle.setNumberOfTrades(((Double)klineEntry.get(8)).intValue());
				binanceCandle.setTakerBuyBaseAssetVolume(klineEntry.get(9).toString());
				binanceCandle.setTakerBuyQuoteAssetVolume(klineEntry.get(10).toString());
				binanceCandle.setIgnore(klineEntry.get(11).toString());
				
				// Map BinanceCandle to your own Candle object
				Candle candle = new Candle();
				candle.setPair(pair);
				candle.setTimeframe(timeframeSec);
				candle.setTime(binanceCandle.getOpenTime());
				candle.setOpen(Double.parseDouble(binanceCandle.getOpen()));
				candle.setClose(Double.parseDouble(binanceCandle.getClose()));
				candle.setMin(Double.parseDouble(binanceCandle.getLow()));
				candle.setMax(Double.parseDouble(binanceCandle.getHigh()));
				candle.setTimeStart(binanceCandle.getOpenTime());
				candle.setTimeEnd(binanceCandle.getCloseTime());
				// Add the candle to the list
				candles.add(candle);
			}
		} catch (Exception e) {
			throw new ExchangeException("Get Candles failed, pair: " + pair + ", timeframeSec: " + timeframeSec + ", startTime: " + startTime + ", endTime: " + endTime + ", exception: " + e);
		}
		return candles;
	}

	@Override
	public DEFAULT_FEE getDefaultFee(boolean buy) {
		return buy ? DEFAULT_FEE.BASE_TOKEN : DEFAULT_FEE.QUOTE_TOKEN;	
	}
	
}
