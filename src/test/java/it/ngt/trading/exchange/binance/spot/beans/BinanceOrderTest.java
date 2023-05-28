package it.ngt.trading.exchange.binance.spot.beans;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import it.ngt.trading.core.ProblemException;
import it.ngt.trading.core.exchange.ExchangeException;

class BinanceOrderTest {

	void loadFromStream() throws IOException, ExchangeException, ProblemException {

		String filenameJson = "src/test/resources/json/binance.straeam.order.executionreport.json";
		String jsonContent = Files.readString(Paths.get(filenameJson));
		System.out.println("jsonContent:\n" + jsonContent);
		
		BinanceOrder order = new BinanceOrder(jsonContent);
		System.out.println("order: " + order);
		
	}

	public static void main(String[] args) throws IOException, ExchangeException, ProblemException {
		
		BinanceOrderTest test = new BinanceOrderTest();
		
		test.loadFromStream();
		
	}
	
}
