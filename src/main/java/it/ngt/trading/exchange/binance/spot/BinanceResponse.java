package it.ngt.trading.exchange.binance.spot;

import it.ngt.trading.core.util.JsonUtil;
import lombok.Data;

@Data
public class BinanceResponse {
	
	public BinanceResponse(String json) {
		
		if (json.startsWith("{\"code\":")) {
			BinanceResponse br = (BinanceResponse) JsonUtil.fromJson(json, this.getClass());
			this.code = br.code;
			this.msg = br.msg;
			error = true;
		} else {
			code = 0;
			msg = "";
			error = false;
		}
	}
	
	private final boolean error;
	
	private final int code;
	
	private final String msg;
	
	public boolean isError() {
		return error;
	}
	
}
