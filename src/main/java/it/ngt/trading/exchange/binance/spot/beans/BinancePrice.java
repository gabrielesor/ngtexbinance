package it.ngt.trading.exchange.binance.spot.beans;

import lombok.Data;

@Data
public class BinancePrice {

	private String symbol;
	
	private String price;
	
}
