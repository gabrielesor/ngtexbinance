package it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo;

import java.util.List;

import lombok.Data;

@Data
public class Symbol {

	  private String symbol;
	  private String status;
	  private String baseAsset;
	  private int baseAssetPrecision;
	  private String quoteAsset;
	  private int quotePrecision;
	  private int quoteAssetPrecision;
	  private int baseCommissionPrecision;
	  private int quoteCommissionPrecision;
	  private List<String> orderTypes;
	  private boolean icebergAllowed;
	  private boolean ocoAllowed;
	  private boolean quoteOrderQtyMarketAllowed;
	  private boolean allowTrailingStop;
	  private boolean cancelReplaceAllowed;
	  private boolean isSpotTradingAllowed;
	  private boolean isMarginTradingAllowed;
	  private List<Filter> filters;
	  private List<String> permissions;
	  private String defaultSelfTradePreventionMode;
	  private List<String> allowedSelfTradePreventionModes;	
	
}
