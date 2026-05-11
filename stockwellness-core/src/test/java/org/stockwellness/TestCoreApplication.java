package org.stockwellness;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "org.stockwellness")
public class TestCoreApplication {
    public void contextLoads() {}
}
