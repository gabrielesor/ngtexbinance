package it.ngt.trading.exchange.binance.spot.beans;

import it.ngt.trading.core.exchange.ITickExchange;
import lombok.Data;

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
@Data
public class BinanceSpotTrade implements ITickExchange {
	
    private String stream;  // Stream name

    private BinanceSpotTradeData data;  // Trade data
    
}