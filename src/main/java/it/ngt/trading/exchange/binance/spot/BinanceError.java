package it.ngt.trading.exchange.binance.spot;

import lombok.Data;

@Data
public class BinanceError {

	private int code;
	
	private String msg;
	
}
