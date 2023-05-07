package it.ngt.trading.exchange.binance.spot.beans;

import lombok.Data;

@Data
public class BinanceTick {
	
    private String symbol;
    private String priceChange;
    private String priceChangePercent;
    private String weightedAvgPrice;
    private String openPrice;
    private String highPrice;
    private String lowPrice;
    private String lastPrice;
    private String volume;
    private String quoteVolume;
    private long openTime;
    private long closeTime;
    private int firstId;
    private int lastId;
    private int count;
    
}

