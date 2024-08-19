package it.ngt.trading.exchange.binance.spot.beans;

import lombok.Data;

@Data
public class BinanceTrade {

    private String symbol;
    private long id;
    private long orderId;
    private int orderListId;
    private String price;
    private String qty;
    private String quoteQty;
    private String commission;
    private String commissionAsset;
    private long time;
    private boolean isBuyer;
    private boolean isMaker;
    private boolean isBestMatch;

}

