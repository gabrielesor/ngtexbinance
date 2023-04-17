package it.ngt.trading.exchange.binance.spot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import it.ngt.trading.core.KrakenException;
import it.ngt.trading.core.entity.Balance;
import it.ngt.trading.core.entity.Order;
import it.ngt.trading.core.entity.Pair;
import it.ngt.trading.exchange.ExchangeException;
import it.ngt.trading.exchange.IExchange;

class BinanceSpotExchangeTest {

	private static final String KEYS_PROPERTIES_FILENAME = "../../../keys/keys.properties";
	private IExchange exchange;

	private Properties properties;

	public BinanceSpotExchangeTest() throws FileNotFoundException, IOException {
		
		this.properties = new Properties();
		properties.load(new FileInputStream(new File(KEYS_PROPERTIES_FILENAME)));
		
	}
	
	private void switchSubaccount(String accountName) {

		String apiKey;
		String apiSecret ;	
		
		apiKey 		= properties.getProperty(accountName + ".apikey");
		if (apiKey == null) {
			throw new KrakenException("apiKey cannot be null, accountName: " + accountName);
		}
		apiSecret 	= properties.getProperty(accountName + ".secretkey");
		if (apiSecret == null) {
			throw new KrakenException("apiSecret cannot be null, accountName: " + accountName);
		}

		this.exchange = new BinanceSpotExchange(accountName, apiKey, apiSecret);
		
		System.out.println("switched subaccount, accountName: " + accountName + ", apiKey: " + apiKey);
	
	}
	
	private void testGetBalances() throws ExchangeException {
		
		Map<String, Balance> balances = this.exchange.getBalances();
		System.out.println("balances: " + balances);
		
	}
	
	private void testGetPairs() throws ExchangeException {
		
		Map<String, Pair> pairs = this.exchange.getPairs();
		System.out.println("pairs: " + pairs);
		
	}
	
	private void testGetOpenOrders() throws ExchangeException {
		
		List<Order> orders = this.exchange.getOpenOrders(null);
		int i=0;
		for(Order order : orders) {
			i++;
			System.out.println("i: " + i + ", order: " + order);
		}
		
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ExchangeException {
		
		BinanceSpotExchangeTest main = new BinanceSpotExchangeTest();
		
		main.switchSubaccount("adara.keys.bit1.binance.main.001");
		//main.switchSubaccount("adara.keys.bseo.binance.main.001");
		
		main.testGetBalances();
		//main.testGetOpenOrders();
		//main.testGetPairs();
		
	}
	
}
