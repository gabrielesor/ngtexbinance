package it.ngt.trading.exchange.binance.spot;

import java.io.IOException;
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
import it.ngt.trading.core.entity.TraderAction;
import it.ngt.trading.core.entity.TraderActionCode;
import it.ngt.trading.core.entity.WayType;
import it.ngt.trading.core.messages.IMessageType;
import it.ngt.trading.core.util.JsonUtil;
import it.ngt.trading.core.util.TimeUtil;
import it.ngt.trading.exchange.ExchangeAbstract;
import it.ngt.trading.exchange.ExchangeErrorCode;
import it.ngt.trading.exchange.ExchangeException;
import it.ngt.trading.exchange.ITickExchange;
import it.ngt.trading.exchange.MarketEnum;
import it.ngt.trading.exchange.binance.spot.beans.BinanceBalance;
import it.ngt.trading.exchange.binance.spot.beans.BinanceConvertOrder;
import it.ngt.trading.exchange.binance.spot.beans.BinanceConvertResponse;
import it.ngt.trading.exchange.binance.spot.beans.BinanceOrder;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.ExchangeInfoSpot;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Filter;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Symbol;
import lombok.extern.slf4j.Slf4j;

/**
 * Api documentation:
 * 	https://docs.binance.us/
 * 	https://binance-docs.github.io/apidocs/spot/en/#order-status-user_data
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
public class BinanceSpotExchange extends ExchangeAbstract {

	private final SpotClientImpl client;

	private final Convert convertClient;
	private final Market marketClient;
	private final Wallet walletClient;
	private final Trade tradeClient;
	
	private Map<String, Pair> pairs;
	
	private static final int DAYS_OFFSET_MAX = 30;	
	
	public BinanceSpotExchange(String accountName, String apiKey, String apiSecret) {
		super(accountName);
		client = new SpotClientImpl(apiKey, apiSecret);
		convertClient = client.createConvert();
		marketClient = client.createMarket();
		tradeClient = client.createTrade();
		walletClient = client.createWallet();
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
	public Map<String, Balance> getBalances() throws ExchangeException {
		
		Map<String, Balance> balancesMap = new TreeMap<>();
		
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "SPOT");		
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
				balance.setCurrencyValuation("BTC");
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

	@Override
	public String doOrderRaw(TraderAction action) throws ExchangeException {
		// TODO Auto-generated method stub
		return null;
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
		
		OrderType orderType = this.buildOrderType(border.getType(), result);
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
		order.setOrderType(this.buildOrderType(border.getType(), result));
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
	
	private OrderType buildOrderType(String btype, String border) {
		
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
	public Map<String, Pair> getPairs() throws ExchangeException {
		
		if (this.pairs != null) {
			if (log.isDebugEnabled()) log.debug("the pairs was already retrieved, numberOfPairs: " + pairs.size());;
			return this.pairs;
		}	
		if (log.isDebugEnabled()) log.debug("retrieving the pairs");
		
		this.pairs = new TreeMap<>();
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
		String result = marketClient.exchangeInfo(parameters);
		try {
			ExchangeInfoSpot ei = (ExchangeInfoSpot) JsonUtil.fromJson(result, ExchangeInfoSpot.class);
			
			List<Symbol> symbols = ei.getSymbols();
			for(Symbol symbol : symbols) {
				
				Float minQuantity = null;
				List<Filter> filters = symbol.getFilters();
				for(Filter filter : filters) {
					switch(filter.getFilterType()) {
					case "LOT_SIZE":
						minQuantity = Float.valueOf(filter.getMinQty());
						break;
					}
				}
				if (minQuantity == null) {
					throw new ExchangeException("missing LOT_SIZE in getPairs, symbol: " + symbol);
				}
				
				Pair pair = new Pair();
				pair.setBase(symbol.getBaseAsset());
				pair.setFeeMaker(0);	//TODO:bseo
				pair.setFeeTaker(0);	//TODO:bseo
				pair.setName(symbol.getSymbol());
				pair.setPriceDecimals(symbol.getQuoteAssetPrecision());
				pair.setQuantityDecimals(symbol.getBaseAssetPrecision());
				pair.setQuantityMin(minQuantity);
				pair.setQuote(symbol.getQuoteAsset());
				pair.setSymbol(symbol.getSymbol());
				pairs.put(pair.getName(), pair);
			}
			
		} catch (JsonProcessingException e) {
			throw new ExchangeException("invalid response in getPairs exception: " + e);
		}
		
		if (log.isDebugEnabled()) log.debug("retrieved the pairs, numberOfPairs: " + pairs.size());
		
		return pairs;
		
	}

}
