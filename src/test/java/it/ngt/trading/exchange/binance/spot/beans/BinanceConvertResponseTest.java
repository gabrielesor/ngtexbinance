package it.ngt.trading.exchange.binance.spot.beans;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import it.ngt.trading.core.ProblemException;

class BinanceConvertResponseTest {

	void loadBean() throws IOException, ProblemException {
		
		String filenameJson = "src/test/resources/json/binance.convert.tradeflow.json";
		String jsonContent = Files.readString(Paths.get(filenameJson));
		System.out.println("jsonContent:\n" + jsonContent);
		
		BinanceConvertResponse response = BinanceConvertResponse.buildFromPayload(jsonContent);
		System.out.println("response: " + response);
		
	}
	public static void main(String[] args) throws IOException, ProblemException {
	
		BinanceConvertResponseTest main = new BinanceConvertResponseTest();
		main.loadBean();
		
	}
	
}
