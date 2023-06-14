package it.ngt.trading.exchange.binance.spot.beans;

import lombok.Data;

/**
     {
        "quoteId": "92f28012d7d744a99b8919e2b30ca05f",
        "orderId": 1416807361880099900,
        "orderStatus": "SUCCESS",
        "fromAsset": "BTC",
        "fromAmount": "0.2975",
        "toAsset": "AVAX",
        "toAmount": "500",
        "ratio": "1680.67",
        "inverseRatio": "0.00059500",
        "createTime": 1679674085884
      } 
*/
@Data
public class BinanceConvertOrder {

	  private String accountCode;
	  private String quoteId;
	  private long orderId;
	  private String orderStatus;
	  private String fromAsset;
	  private String fromAmount;
	  private String toAsset;
	  private String toAmount;
	  private String ratio;
	  private String inverseRatio;
	  private long createTime;	
	  private String createTimeFormatted;
	
}
