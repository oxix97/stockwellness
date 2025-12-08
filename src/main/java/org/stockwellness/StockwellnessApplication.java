package org.stockwellness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StockwellnessApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockwellnessApplication.class, args);
	}

}
