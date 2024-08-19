package it.ngt.trading.exchange.binance.spot.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonObject;

import it.ngt.trading.core.ProblemException;
import it.ngt.trading.core.exchange.ExchangeException;
import it.ngt.trading.core.util.JsonUtil;
import lombok.Data;

@Data
public class BinanceOrder {
	
	public BinanceOrder() {
	}
	
	/*
	  "e": "executionReport",        // Event type
	  "E": 1499405658658,            // Event time
	  "s": "ETHBTC",                 // Symbol
	  "c": "mUvoqJxFIILMdfAW5iGSOW", // Client order ID
	  "S": "BUY",                    // Side
	  "o": "LIMIT",                  // Order type
	  "f": "GTC",                    // Time in force
	  "q": "1.00000000",             // Order quantity
	  "p": "0.10264410",             // Order price
	  "P": "0.00000000",             // Stop price
	  "d": 4,                        // Trailing Delta; This is only visible if the order was a trailing stop order.
	  "F": "0.00000000",             // Iceberg quantity
	  "g": -1,                       // OrderListId
	  "C": "",                       // Original client order ID; This is the ID of the order being canceled
	  "x": "NEW",                    // Current execution type
	  "X": "NEW",                    // Current order status
	  "r": "NONE",                   // Order reject reason; will be an error code.
	  "i": 4293153,                  // Order ID
	  "l": "0.00000000",             // Last executed quantity
	  "z": "0.00000000",             // Cumulative filled quantity
	  "L": "0.00000000",             // Last executed price
	  "n": "0",                      // Commission amount
	  "N": null,                     // Commission asset
	  "T": 1499405658657,            // Transaction time
	  "t": -1,                       // Trade ID
	  "I": 8641984,                  // Ignore
	  "w": true,                     // Is the order on the book?
	  "m": false,                    // Is this trade the maker side?
	  "M": false,                    // Ignore
	  "O": 1499405658657,            // Order creation time
	  "Z": "0.00000000",             // Cumulative quote asset transacted quantity
	  "Y": "0.00000000",             // Last quote asset transacted quantity (i.e. lastPrice * lastQty)
	  "Q": "0.00000000",             //Quote Order Quantity
	  "V": "selfTradePreventionMode",
	  "D": "trailing_time",          // (Appears if the trailing stop order is active)
	  "W": "workingTime"             // (Appears if the order is working on the order book)
	  "u":12332                      // tradeGroupId (Appear if the order has expired due to STP)
	  "v":122                        // preventedMatchId (Appear if the order has expired due to STP)
	  "U":2039                       // counterOrderId (Appear if the order has expired due to STP)
	  "A":"1.00000000"               // preventedQuantity(Appear if the order has expired due to STP )
	  "B":"2.00000000"               // lastPreventedQuantity(Appear if the order has expired due to STP)
	}	
	 */
	public BinanceOrder(String executioReport) throws ExchangeException, ProblemException {
		
		JsonObject jo = (JsonObject) JsonUtil.fromJson(executioReport, JsonObject.class);
		symbol = jo.get("s").getAsString();
		orderId = jo.get("i").getAsInt();
		orderListId = jo.get("g").getAsInt();
		clientOrderId = jo.get("c").getAsString();
		price = jo.get("p").getAsString();
		origQty = jo.get("q").getAsString();
		executedQty = "";	//TODO:check
		cummulativeQuoteQty = jo.get("z").getAsString();
		status = jo.get("X").getAsString();
		timeInForce = jo.get("f").getAsString();
		type = jo.get("o").getAsString();
		side = jo.get("S").getAsString();
		stopPrice = jo.get("P").getAsString();
		icebergQty = jo.get("F").getAsString();
		time = jo.get("E").getAsLong();
		updateTime = 0;	//TODO:check
		isWorking = false; //TODO:check
		workingTime = 0; //TODO:check
		origQuoteOrderQty = jo.get("Q").getAsString();
		selfTradePreventionMode = jo.get("V").getAsString();
		
	}
	
	private String symbol;

	private long orderId;

	private long orderListId;

	private String clientOrderId;

	private String price;

	@JsonProperty("origQty")
	private String origQty;

	private String executedQty;

	private String cummulativeQuoteQty;

	private String status;

	private String timeInForce;

	private String type;

	private String side;

	private String stopPrice;

	private String icebergQty;

	private long time;

	private long updateTime;

	private boolean isWorking;

	private long workingTime;

	private String origQuoteOrderQty;

	private String selfTradePreventionMode;
	
}
