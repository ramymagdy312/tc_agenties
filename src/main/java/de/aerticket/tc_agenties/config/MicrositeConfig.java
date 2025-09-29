package de.aerticket.tc_agenties.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "microsite")
@Data
public class MicrositeConfig {

    private String fallbackUrl = "https://de.aer360.travel/";
		
    private String fallbackMicrosite = "aer360";

    private String fallbackMicrositeAPI = "aer360";

}