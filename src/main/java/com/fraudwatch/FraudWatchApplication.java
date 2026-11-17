package com.fraudwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FraudWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudWatchApplication.class, args);
    }
}
