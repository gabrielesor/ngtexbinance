package it.ngt.trading.exchange.binance.spot.beans;

import lombok.Data;

@Data
public class BinanceCandle {
	
    private long openTime;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private long closeTime;
    private String quoteAssetVolume;
    private int numberOfTrades;
    private String takerBuyBaseAssetVolume;
    private String takerBuyQuoteAssetVolume;
    private String ignore;

}
