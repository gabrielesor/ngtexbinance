package it.ngt.trading.exchange.binance.spot.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BinanceBalance {

    private String asset;
    private String free;
    private String locked;
    private String freeze;
    private String withdrawing;
    private String ipoable;
    private String btcValuation;
	
}
