package it.ngt.trading.exchange.binance.spot;

import lombok.Data;

@Data
public class BinanceSystemStatusResponse {

	private int status;
	
	private String msg;
	
}
