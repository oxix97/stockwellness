package org.stockwellness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "org.stockwellness")
@ConfigurationPropertiesScan(basePackages = "org.stockwellness")
public class StockwellnessBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockwellnessBatchApplication.class, args);
    }

}
