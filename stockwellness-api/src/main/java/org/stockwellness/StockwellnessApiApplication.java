package org.stockwellness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(scanBasePackages = "org.stockwellness")
@ConfigurationPropertiesScan(basePackages = "org.stockwellness")
public class StockwellnessApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockwellnessApiApplication.class, args);
	}

}
