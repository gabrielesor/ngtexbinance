package it.ngt.trading.exchange.binance.spot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.ngt.trading.core.util.JsonUtil;

public class BinancePairsTest {
	
	public static void main(String[] args) throws IOException {
		
		String pairsSpot = Files.readString(Paths.get("src/test/resources/json/binance.exchangeinfo.json"));		
		JsonObject joPairsSpot = (JsonObject) JsonUtil.fromJson(pairsSpot, JsonObject.class);
		JsonElement symbolsE = joPairsSpot.get("symbols");
		JsonArray symbols = symbolsE.getAsJsonArray();
		Set<String> pairsSpotSet = new TreeSet<>();
		symbols.forEach(symbolE -> {
			String symbol = symbolE.getAsJsonObject().get("symbol").getAsString();
			String b = symbolE.getAsJsonObject().get("symbol").getAsString();
			pairsSpotSet.add(symbol);
		});
		System.out.println("pairsSpotSet.size(): " + pairsSpotSet.size());
		
		String pairsConvert = Files.readString(Paths.get("src/test/resources/json/binance.convert.exchangeinfo.json"));
		JsonArray joPairsConvert = (JsonArray) JsonUtil.fromJson(pairsConvert, JsonArray.class);
		System.out.println("x1: " + joPairsConvert.size());
		Set<String> pairsConvertSet = new TreeSet<>();
		joPairsConvert.forEach(pairConvert -> {
			pairsConvertSet.add(pairConvert.getAsJsonObject().get("fromAsset").getAsString());
		});
		System.out.println("pairsConvertSet.size(): " + pairsConvertSet.size());
		
	}
	
	
}
