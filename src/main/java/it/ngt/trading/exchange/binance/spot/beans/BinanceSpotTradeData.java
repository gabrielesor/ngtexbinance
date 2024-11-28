package it.ngt.trading.exchange.binance.spot.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
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
@Data
public class BinanceSpotTradeData {
	
    private String e;  // Event type

    @JsonProperty("E")
    private Long EC;  // Event time

    private String s;  // Symbol

    private Long t;  // Trade ID

    private String p;  // Price

    private String q;  // Quantity

    private Long b;  // Buyer order ID

    private Long a;  // Seller order ID

    @JsonProperty("T")
    private Long TC;  // Trade time

    private Boolean m;  // Is buyer the market maker?

    @JsonProperty("M")
    private Boolean MC;  // Ignore (always present, but unused)
    
}