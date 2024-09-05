package it.ngt.trading.exchange.binance.spot.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

public class JsonTest {

	@Data
	static class A {
		private int a;
		@JsonProperty("A")
		private int capitalA;
	}
	public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
		
		ObjectMapper mapper = new ObjectMapper();
		String json = "{\"a\":1,\"A\":2}";

		A aObject = mapper.readValue(json, A.class);

		System.out.println(aObject.getA()); // Output: 1
		System.out.println(aObject.getCapitalA()); // Output: 2		
		
	}
}
