package it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo;

import lombok.Data;

@Data
public class Filter{
	
	  private String filterType;
	  private String minPrice;
	  private String maxPrice;
	  private String tickSize;
	  private String minQty;
	  private String maxQty;
	  private String stepSize;
	  private String minNotional;
	  private boolean applyToMarket;
	  private int avgPriceMins;
	  private int limit;
	  private int minTrailingAboveDelta;
	  private int maxTrailingAboveDelta;
	  private int minTrailingBelowDelta;
	  private int maxTrailingBelowDelta;
	  private String bidMultiplierUp;
	  private String bidMultiplierDown;
	  private String askMultiplierUp;
	  private String askMultiplierDown;
	  private int maxNumOrders;
	  private int maxNumAlgoOrders;
	  
	}