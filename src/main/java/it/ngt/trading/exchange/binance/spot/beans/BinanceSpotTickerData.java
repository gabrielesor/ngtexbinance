package it.ngt.trading.exchange.binance.spot.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.ngt.trading.core.util.JsonUtil;
import lombok.Data;

/**
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
@Data
public class BinanceSpotTickerData {
	
    private String e;  // Event type
    @JsonProperty("E")
    private Long EC;    // Event time
    private String s;  // Symbol
    private String p;  // Price change
    @JsonProperty("P")
    private String PC;  // Price change percent
    private String w;  // Weighted average price
    private String x;  // Previous day's close price
    private String c;  // Current day's close price    
    @JsonProperty("Q")
    private String QC;  // Close trade's quantity
    private String b;  // Best bid price
    @JsonProperty("B")
    private String BC;  // Best bid quantity
    private String a;  // Best ask price
    @JsonProperty("A")
    private String AC;  // Best ask quantity
    private String o;  // Open price
    private String h;  // High price
    private String l;  // Low price
    private String v;  // Total traded base asset volume
    private String q;  // Total traded quote asset volume
    @JsonProperty("O")
    private Long OC;    // Statistics open time
    @JsonProperty("C")
    private Long CC;    // Statistics close time
    @JsonProperty("F")
    private Long FC;    // First trade ID
    @JsonProperty("L")
    private Long LC;    // Last trade ID
    private Long n;    // Total number of trades
    
    public static void main(String[] args) {

    	String json = "{\"stream\":\"btceur@ticker\",\"data\":{\"e\":\"24hrTicker\",\"E\":1724595459892,\"s\":\"BTCEUR\",\"p\":\"-136.41000000\",\"P\":\"-0.238\",\"w\":\"57103.95279063\",\"x\":\"57263.40000000\",\"c\":\"57132.49000000\",\"Q\":\"0.00007000\",\"b\":\"57130.42000000\",\"B\":\"0.00576000\",\"a\":\"57132.49000000\",\"A\":\"0.00607000\",\"o\":\"57268.90000000\",\"h\":\"57599.00000000\",\"l\":\"56579.30000000\",\"v\":\"107.58053000\",\"q\":\"6143273.50631090\",\"O\":1724509059891,\"C\":1724595459891,\"F\":136299847,\"L\":136318457,\"n\":18611}}";
    	BinanceSpotTicker bst = (BinanceSpotTicker) JsonUtil.fromJsonJackson(json, BinanceSpotTicker.class);
    	System.out.println("bst: " + bst);
    	System.out.println(Double.valueOf(bst.getData().getA()));
	}

}
