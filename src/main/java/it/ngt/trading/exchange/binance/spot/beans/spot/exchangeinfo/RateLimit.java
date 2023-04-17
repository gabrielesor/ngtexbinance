package it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo;

import lombok.Data;

@Data
public class RateLimit {
	
	private String rateLimitType;
	private String interval;
	private int intervalNum;
	private int limit;

}