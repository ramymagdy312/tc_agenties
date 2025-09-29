package de.aerticket.tc_agenties.service;

import de.aerticket.tc_agenties.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class HttpClientService {

    private final JwtConfig jwtConfig;
    private final ConcurrentHashMap<String, String> publicKeyCache = new ConcurrentHashMap<>();

    public String getPublicKey(String keyUrl) {
        // Check cache first
        String cachedKey = publicKeyCache.get(keyUrl);
        if (cachedKey != null) {
            log.debug("Retrieved public key from cache for URL: {}", keyUrl);
            return cachedKey;
        }

        try {
            log.info("Fetching public key from URL: {}", keyUrl);

            URL url = new URL(keyUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set timeouts
            connection.setConnectTimeout(jwtConfig.getConnectionTimeout());
            connection.setReadTimeout(jwtConfig.getReadTimeout());
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AER-TC-Agencies/1.0");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                String publicKey = response.toString();

                // Cache the public key
                publicKeyCache.put(keyUrl, publicKey);

                log.info("Successfully retrieved and cached public key from: {}", keyUrl);
                return publicKey;

            } else {
                log.error("Failed to retrieve public key. HTTP response code: {} for URL: {}",
                        responseCode, keyUrl);
                return null;
            }

        } catch (IOException e) {
            log.error("Error retrieving public key from URL: {} - {}", keyUrl, e.getMessage());
            return null;
        }
    }

    public void clearCache() {
        publicKeyCache.clear();
        log.info("Public key cache cleared");
    }
}