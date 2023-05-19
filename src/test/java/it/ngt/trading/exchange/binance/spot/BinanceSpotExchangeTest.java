package it.ngt.trading.exchange.binance.spot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import it.ngt.trading.core.EngineException;
import it.ngt.trading.core.entity.Balance;
import it.ngt.trading.core.entity.Order;
import it.ngt.trading.core.entity.Pair;
import it.ngt.trading.core.entity.Price;
import it.ngt.trading.exchange.ExchangeException;
import it.ngt.trading.exchange.IExchange;
import it.ngt.trading.exchange.binance.spot.beans.BinancePrice;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Filter;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Symbol;

class BinanceSpotExchangeTest {

	private static final String KEYS_PROPERTIES_FILENAME = "../../../keys/keys.properties";
	private IExchange exchange;

	private Properties properties;

	public BinanceSpotExchangeTest() throws FileNotFoundException, IOException {
		
		this.properties = new Properties();
		properties.load(new FileInputStream(new File(KEYS_PROPERTIES_FILENAME)));
		
	}
	
	private void switchSubaccount(String accountName) throws ExchangeException {

		String apiKey;
		String apiSecret ;	
		
		apiKey 		= properties.getProperty(accountName + ".apikey");
		if (apiKey == null) {
			throw new EngineException("apiKey cannot be null, accountName: " + accountName);
		}
		apiSecret 	= properties.getProperty(accountName + ".secretkey");
		if (apiSecret == null) {
			throw new EngineException("apiSecret cannot be null, accountName: " + accountName);
		}

		this.exchange = new BinanceSpotExchange(accountName, apiKey, apiSecret);
		
		System.out.println("switched subaccount, accountName: " + accountName + ", apiKey: " + apiKey);
	
	}
	
	private void testGetBalances() throws ExchangeException {
		
		Map<String, Balance> balances = this.exchange.getBalances();
		System.out.println("balances: " + balances);
		
	}
	
	private void testGetPair() throws ExchangeException {
		
		Pair pair = this.exchange.getPair("BTC", "EUR");
		System.out.println("pair: " + pair);
		
	}
	
	private void testGetPairs() throws ExchangeException {
		
		Map<String, Pair> pairs = this.exchange.getPairsMap();
		System.out.println("numberOfPairs: " + pairs.size());
		System.out.println("pair: " + pairs.get("XRPEUR"));
		
	}
	
	private void testGetOpenOrders() throws ExchangeException {
		
		List<Order> orders = this.exchange.getOpenOrders(null);
		int i=0;
		for(Order order : orders) {
			i++;
			System.out.println("i: " + i + ", order: " + order);
		}
		
	}
	
	private void testGetPrices() throws ExchangeException {
		
		BinanceSpotExchange exchange = (BinanceSpotExchange) this.exchange;
		
		Map<String, Price> prices = exchange.getPricesMap();
		prices.forEach((asset, price) -> {
			System.out.println("price: " + price);			
		});
		
	}
	
	private void testGetSymbols() throws ExchangeException {
		
		BinanceSpotExchange exchange = (BinanceSpotExchange) this.exchange;
		List<Symbol> symbols = exchange.getSymbols();
		
		System.out.println("numberOfSymbols: " + symbols.size());
		
		int i=0;
		
		//
		// check lot_size
		//
		if (false) {
			System.out.println("\n\n\ncheck lot size2");
			for(Symbol symbol : symbols) {
				List<Filter> filters = symbol.getFilters();			
				for(Filter filter : filters) {
					if (filter.getFilterType().equals("LOT_SIZE")) {
						String stepSize = filter.getStepSize();
						Float stepSizeD = Float.valueOf(stepSize);
						System.out.println("lotsize2:\t" +  symbol.getSymbol() + "\t" + filter.getStepSize() + "\t" + BigDecimal.valueOf(stepSizeD).scale() + ", " + stepSizeD);
					}
				}			
			}				
		}
		
		//
		// check lot_size
		//
		System.out.println("\n\n\ncheck lot size");
		for(Symbol symbol : symbols) {
			List<Filter> filters = symbol.getFilters();			
			for(Filter filter : filters) {
				if (filter.getFilterType().equals("PRICE_FILTER")) {
					String stepSize = filter.getTickSize();
					if (false
					||	stepSize.equals("10.00000000")
					||	stepSize.equals("1.0")
					||	stepSize.equals("1.00")
					||	stepSize.equals("1.00000000")
					||  stepSize.equals("0.10")
					||  stepSize.equals("0.10000000")
					||  stepSize.equals("0.01")
					||  stepSize.equals("0.01000000")
					||  stepSize.equals("0.00100000")
					||  stepSize.equals("0.00010000")
					||  stepSize.equals("0.00001000")
					||  stepSize.equals("0.00000100")
					||  stepSize.equals("0.00000010")
					||  stepSize.equals("0.00000001")
					) {		
					} else {
						System.err.println("stepSize: " + symbol.getSymbol() + ", " + stepSize);
					}
				}
			}			
		}
		
		if (true) return;
		
		//
		// print minimum quote quantity
		//
		System.out.println("\n\n\nNOTIONAL");
		i=0;
		for(Symbol symbol : symbols) {
			i++;
			if (symbol.getQuoteAsset().equals("EUR")) {
				List<Filter> filters = symbol.getFilters();
				for(Filter filter : filters) {
					if (filter.getFilterType().equals("NOTIONAL")) {
						System.out.println("notional: " + symbol.getSymbol() + ", " + filter.getMinNotional());
					}
				}
			}
		}
		//
		// print minimum quote quantity
		//
		System.out.println("\n\n\nLOT_SIZE");
		i=0;
		for(Symbol symbol : symbols) {
			i++;
			if (symbol.getQuoteAsset().equals("EUR")) {
				List<Filter> filters = symbol.getFilters();
				for(Filter filter : filters) {
					if (filter.getFilterType().equals("LOT_SIZE")) {
						System.out.println("lotSize: " + symbol.getSymbol() + ", " + filter.getStepSize());
					}
				}
			}
		}		
		
	}
	
	private void testPairsAndPrices() throws ExchangeException {
		
		Map<String, Pair> pairs = this.exchange.getPairsMap();
		Map<String, Price> prices = this.exchange.getPricesMap();
		System.out.println("pairsSize: " + pairs.size() + ", pricesSize: " + prices.size());
		pairs.forEach((pairName, pair) -> {
			if (!prices.containsKey(pairName)) {
				System.out.println("prices not in pairs, pairName: " + pairName);
			}
		});
	}	
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ExchangeException {
		
		BinanceSpotExchangeTest main = new BinanceSpotExchangeTest();
		
		//main.switchSubaccount("adara.keys.bit1.binance.main.001");
		main.switchSubaccount("adara.keys.bit1.binance.sub001.001");
		
		main.testGetBalances();
		//main.testGetOpenOrders();
		//main.testGetPair();
		//main.testGetPairs();
		//main.testGetPrices();
		//main.testGetSymbols();
		//main.testPairsAndPrices();
		
	}
	
}
