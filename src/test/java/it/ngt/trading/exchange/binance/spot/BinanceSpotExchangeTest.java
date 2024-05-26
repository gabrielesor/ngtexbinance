package it.ngt.trading.exchange.binance.spot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import it.ngt.trading.core.EngineException;
import it.ngt.trading.core.ProblemException;
import it.ngt.trading.core.entity.Asset;
import it.ngt.trading.core.entity.Balance;
import it.ngt.trading.core.entity.ExchangeStatus;
import it.ngt.trading.core.entity.Order;
import it.ngt.trading.core.entity.Pair;
import it.ngt.trading.core.entity.Price;
import it.ngt.trading.core.exchange.ExchangeException;
import it.ngt.trading.core.exchange.IExchange;
import it.ngt.trading.core.util.FormatUtil;
import it.ngt.trading.exchange.binance.spot.beans.BinancePrice;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Filter;
import it.ngt.trading.exchange.binance.spot.beans.spot.exchangeinfo.Symbol;

class BinanceSpotExchangeTest {

	private static final String KEYS_PROPERTIES_FILENAME = "../../../keys/keys.properties.bit1";
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
	
	private void testGetBalances() throws ProblemException {
		
		this.exchange.addAssetDelisted("XMR");
		Map<String, Balance> balances = this.exchange.getBalancesMap();
		System.out.println("balances: " + balances);
		
	}
	
	private void testGetPair() throws ExchangeException {
		
		Pair pair = this.exchange.getPair("BTC", "EUR");
		System.out.println("pair: " + pair);
		
	}
	
	private void testGetPairs() throws ProblemException {
		
		Map<String, Pair> pairs = this.exchange.getPairsMap();
		System.out.println("numberOfPairs: " + pairs.size());
		System.out.println("pair: " + pairs.get("XRPEUR"));
		
	}
	
	private void testGetOpenOrders() throws ExchangeException, ProblemException {
		
		List<Order> orders = this.exchange.getOpenOrders(null);
		int i=0;
		for(Order order : orders) {
			i++;
			System.out.println("i: " + i + ", order: " + order);
		}
		
	}
	
	private void testGetOrders() throws ExchangeException, ProblemException {
		
		System.out.println("getOrders started");
		List<Order> orders = this.exchange.getOrders("RUNEETH");
		System.out.println("getOrders executed, numberOfOrders: " + orders.size());
		int i=0;
		for(Order order : orders) {
			i++;
			System.out.println("i: " + i + ", order: " + order);
		}
		System.out.println("getOrders ended");
		
	}
	
	private void testGetPrices() throws ProblemException {
		
		BinanceSpotExchange exchange = (BinanceSpotExchange) this.exchange;
		
		Map<String, Price> prices = exchange.getPricesMap();
		prices.forEach((asset, price) -> {
			System.out.println("price: " + price);			
		});
		
	}
	
	private void testGetOrder(long orderId) throws ProblemException {
		
		BinanceSpotExchange exchange = (BinanceSpotExchange) this.exchange;
		
		Order order = exchange.getOrder(orderId + "", "BTCEUR");
		System.out.println("order: " + order);
		
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
	
	private void testGetExchangeStatus() {
		
		System.out.println("date.a: " + new Date().getTime());
		ExchangeStatus status = this.exchange.getExchangeStatus();
		System.out.println("date.b: " + new Date().getTime());
		System.out.println("status: " + status);
	}
	
	private void testPairsAndPrices() throws ProblemException {
		
		Map<String, Pair> pairs = this.exchange.getPairsMap();
		Map<String, Price> prices = this.exchange.getPricesMap();
		System.out.println("pairsSize: " + pairs.size() + ", pricesSize: " + prices.size());
		pairs.forEach((pairName, pair) -> {
			if (!prices.containsKey(pairName)) {
				System.out.println("prices not in pairs, pairName: " + pairName);
			}
		});
	}
		
	private void testPricesDerived() {

		List<Asset> assets = this.exchange.getAssets();
		List<Pair> pairs = this.exchange.getPairs();
		List<Price> prices = this.exchange.getPrices();
		
		int numberOfAssets = assets.size();
		int numberOfPairs = pairs.size();
		int numberOfPrices = prices.size();
		
		System.out.println("numberOfAssets: " + numberOfAssets);
		System.out.println("numberOfPairs: " + numberOfPairs);
		System.out.println("numberOfPrices: " + numberOfPrices);
	
		Set<String> pairsNamesExistent1 = new TreeSet<>();
		for(Pair pair : pairs) {
			pairsNamesExistent1.add(pair.getName());
		}
		
		Set<String> pairsNamesExistent2 = new TreeSet<>();
		for(int i=0; i<numberOfAssets; i++) {
			Asset asset1 = assets.get(i);
			for(int j=0; j<numberOfAssets; j++) {
				Asset asset2 = assets.get(j);
				if (exchange.isPairExist(asset1.getName(), asset2.getName())) {
					pairsNamesExistent2.add(asset1.getName() +  asset2.getName());
				} else {
				}
			}
		}
		 
		System.out.println("pairsNamesExistent1.size: " + pairsNamesExistent1.size());		
		System.out.println("pairsNamesExistent2.size: " + pairsNamesExistent2.size());		
		
		System.out.println("universal price BTC");
		for(int i=0; i<numberOfAssets; i++) {
			Asset asset = assets.get(i);
			double price = exchange.getPriceUniversal(asset.getName(), "BTC");
			System.out.println(asset.getName() + "\t" + FormatUtil.formatDecimalsMax(price));
		}		
		
		System.out.println("universal price EUR");
		for(int i=0; i<numberOfAssets; i++) {
			Asset asset = assets.get(i);
			double price = exchange.getPriceUniversal(asset.getName(), "EUR");
			System.out.println(asset.getName() + "\t" + FormatUtil.formatConversion(price));
		}

	}	
	
	private boolean confirm(String message) {
		Scanner scanner = new Scanner(System.in);
		System.out.println("> " + message);
		return scanner.nextLine().toLowerCase().equals("y");		
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ExchangeException, ProblemException {
		
		BinanceSpotExchangeTest main = new BinanceSpotExchangeTest();
		
		// REMEMBER TO SET TO false
		// REMEMBER TO SET TO false
		// REMEMBER TO SET TO false
		// REMEMBER TO SET TO false
		boolean doAction = true;
		// REMEMBER TO SET TO false
		// REMEMBER TO SET TO false
		// REMEMBER TO SET TO false
		// REMEMBER TO SET TO false		
		
		if (doAction) {
			if (main.confirm("Confirm to proceed with this Proof?")) {	

			} else {
				System.out.println("ACTION not confirmed");
				System.out.println("Proof ended with no action");
				return;
			}
		} else {
			System.out.println("NO PROCEED did, the flag was false, if you really want do this action then set it to true");
			System.out.println("Proof ended with no action");
			return;
		}	
		
		main.switchSubaccount("adara.keys.bit1.binance.01.a");
		//main.switchSubaccount("adara.keys.bseo.binance.main.001");
		//main.switchSubaccount("adara.keys.ec.binance.01.a");
		
		//main.testGetBalances();
		//main.testGetExchangeStatus();
		//main.testGetOpenOrders();
		//main.testGetOrder(2838503151l);	//BSEO-BN-02, found
		//main.testGetOrder(2838503888l);		//BSEO-BN-02, not found
		//main.testGetPair();
		//main.testGetPairs();
		//main.testGetPrices();
		//main.testGetSymbols();
		main.testGetOrders();
		//main.testPairsAndPrices();
		//main.testPricesDerived();
		
	}
	
}
