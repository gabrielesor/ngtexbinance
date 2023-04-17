package it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo;

import java.util.List;

import lombok.Data;

@Data
public class ExchangeInfoSpot {

	private String timezone;
	private long serverTime;
	private List<RateLimit> rateLimits;
	private List<Object> exchangeFilters;
	private List<Symbol> symbols;

}
