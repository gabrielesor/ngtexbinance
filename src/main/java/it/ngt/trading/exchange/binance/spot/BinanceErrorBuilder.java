package it.ngt.trading.exchange.binance.spot;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;

import it.ngt.trading.core.util.JsonUtil;
import it.ngt.trading.exchange.ExchangeErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BinanceErrorBuilder {

	private static Map<Integer, ExchangeErrorCode> codesMap = new HashMap<>();
	static {
		codesMap.put(-1121, ExchangeErrorCode.PAIR_INVALID);
	}
	
	public static ExchangeErrorCode getExchangeErrorCode(int errorCode) {
		
		if (log.isDebugEnabled()) log.debug("getErrorCode started, errorCode: " + errorCode);
			
		
		ExchangeErrorCode code = codesMap.get(errorCode);
		if (code == null) {
			code = ExchangeErrorCode.EXCHANGE_GENERIC;
		}

		if (log.isDebugEnabled()) log.debug("getErrorCode ended, code: " + code);
		
		return code;
	}
}
