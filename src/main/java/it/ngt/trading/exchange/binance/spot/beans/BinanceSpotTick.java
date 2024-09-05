package it.ngt.trading.exchange.binance.spot.beans;

import it.ngt.trading.core.exchange.ITickExchange;
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
public class BinanceSpotTick implements ITickExchange {
	
	private String stream;
	private BinanceSpotTickData data;

}
