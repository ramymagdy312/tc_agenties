package de.aerticket.tc_agenties.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    private String secretKey = "BWa5fXSRPzdphmMXftKSdyykcgrjJWBa7JJCgLu33ZfqJn6tboN0DfsPycvSyqEV";

    private String qaPublicKeyUrl = "https://qa-cockpit-aer-de.aerticket.org/common/keys/{kid}.pub";
    private String stgPublicKeyUrl = "https://stg-cockpit-aer-de.aerticket.org/common/keys/{kid}.pub";
    private String prodPublicKeyUrl = "https://cockpit.aerticket.de/common/keys/{kid}.pub";

    private int connectionTimeout = 5000; // 5 seconds
    private int readTimeout = 10000; // 10 seconds
}