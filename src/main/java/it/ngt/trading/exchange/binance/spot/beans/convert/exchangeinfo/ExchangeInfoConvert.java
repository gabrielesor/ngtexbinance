package it.ngt.trading.exchange.binance.spot.beans.convert.exchangeinfo;

import lombok.Data;

@Data
public class ExchangeInfoConvert {
	
    private String fromAsset;
    private String toAsset;
    private String fromAssetMinAmount;
    private String fromAssetMaxAmount;
    private String toAssetMinAmount;
    private String toAssetMaxAmount;
    
}
