package it.ngt.trading.exchange.binance.spot.beans;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import it.ngt.trading.core.ProblemException;
import it.ngt.trading.core.util.JsonUtil;
import lombok.Data;

@Data
public class BinanceConvertResponse {

	public BinanceConvertResponse() {
	}

	/**
		{
		    "list": [
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
		      },
		      {
		        "quoteId": "977422b34aeb4be7acba1d191079f841",
		        "orderId": 1416808152153884462,
		        "orderStatus": "SUCCESS",
		        "fromAsset": "BTC",
		        "fromAmount": "0.2518",
		        "toAsset": "LINK",
		        "toAmount": "1000",
		        "ratio": "3971.41",
		        "inverseRatio": "0.00025180",
		        "createTime": 1679674177763
		      }
		    ],
		    "startTime": 1679616000000,
		    "endTime": 1682207999000,
		    "limit": 100,
		    "moreData": false
		  }	 
	 * @throws ProblemException 
	 */
	public static BinanceConvertResponse buildFromPayload(String payload) throws IOException, ProblemException {
		return (BinanceConvertResponse) JsonUtil.fromJson(payload, BinanceConvertResponse.class);
	}
	
	List<BinanceConvertOrder> list;
	private float startTime;
	private float endTime;
	private float limit;
	private boolean moreData;

}
