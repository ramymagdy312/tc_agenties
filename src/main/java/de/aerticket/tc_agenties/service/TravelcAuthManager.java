package de.aerticket.tc_agenties.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TravelcAuthManager {

	private static final String BASE_URL = "https://kombireisen.suntrips.de/resources/";
	private static final String AUTH_ENDPOINT = "authentication/authenticate";
	private static final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
	private static final Map<String, Credentials> CREDENTIALS = new ConcurrentHashMap<>();
	private static final int TOKEN_TIMEOUT_SECONDS = 30 * 60; // 30 minutes

	static {
		// Initialize credentials for different microsites
		CREDENTIALS.put("aer360", new Credentials("Rocket_API_user", "6^ODD^Jb^3sZe^Sd"));
	}

	public static String getToken(String microsite) {
		if (microsite == null) {
			log.warn("Microsite is null, cannot get token");
			return null;
		}

		String key = microsite.toLowerCase();
		log.debug("Getting token for microsite: {}", key);

		TokenInfo tokenInfo = tokens.get(key);
		if (tokenInfo != null && !tokenInfo.isExpired()) {
			log.debug("Token found and valid for microsite: {}", key);
			return tokenInfo.getToken();
		}

		log.debug("Token expired or not found. Requesting new one for microsite: {}", key);
		Credentials creds = CREDENTIALS.get(key);
		if (creds == null) {
			log.error("No credentials found for microsite: {}", key);
			return null;
		}

		String token = fetchTokenFromApi(key, creds.username, creds.password);
		if (token != null) {
			tokens.put(key, new TokenInfo(token));
			log.info("New token obtained and cached for microsite: {}", key);
		}

		return token;
	}

	private static String fetchTokenFromApi(String microsite, String username, String password) {
		log.info("Fetching token for user: {} microsite: {}", username, microsite);

		if (username == null || password == null || microsite == null) {
			log.error("Missing credentials for token request");
			return null;
		}

		try {
			RestTemplate restTemplate = new RestTemplate();
			String apiUrl = BASE_URL + AUTH_ENDPOINT;

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Accept", "application/json");

			// Create request body
			String requestBody = String.format("{\"username\":\"%s\",\"password\":\"%s\",\"micrositeId\":\"%s\"}",
					username, password, microsite);

			HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				log.error("Token fetch failed. HTTP: {}", response.getStatusCode());
				return null;
			}

			// Parse response to extract token
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response.getBody());
			JsonNode tokenNode = root.get("token");

			if (tokenNode == null || tokenNode.isNull()) {
				log.error("No token found in API response");
				return null;
			}

			String token = tokenNode.asText();
			log.info("Token successfully fetched for microsite: {}", microsite);
			return token;

		} catch (Exception e) {
			log.error("Exception during token fetch for microsite {}: {}", microsite, e.getMessage());
			return null;
		}
	}

	/**
	 * Get authentication token for the specified microsite
	 */
	public String getAuthToken(String microsite) {
		return getToken(microsite);
	}

	/**
	 * Validate if the token is still valid
	 */
	public boolean isTokenValid(String token) {
		return token != null && !token.isEmpty();
	}

	/**
	 * Refresh the authentication token
	 */
	public String refreshToken(String microsite) {
		log.info("Refreshing auth token for microsite: {}", microsite);
		// Remove existing token to force refresh
		if (microsite != null) {
			tokens.remove(microsite.toLowerCase());
		}
		return getToken(microsite);
	}

	/**
	 * Clear all cached tokens
	 */
	public void clearAllTokens() {
		log.info("Clearing all cached tokens");
		tokens.clear();
	}

	/**
	 * Check if credentials exist for microsite
	 */
	public boolean hasCredentials(String microsite) {
		return microsite != null && CREDENTIALS.containsKey(microsite.toLowerCase());
	}

	// Inner static classes
	private static class TokenInfo {
		private final String token;
		private final Instant issuedAt;

		public TokenInfo(String token) {
			this.token = token;
			this.issuedAt = Instant.now();
		}

		public String getToken() {
			return token;
		}

		public boolean isExpired() {
			return Instant.now().isAfter(issuedAt.plusSeconds(TOKEN_TIMEOUT_SECONDS));
		}
	}

	private static class Credentials {
		public final String username;
		public final String password;

		public Credentials(String username, String password) {
			this.username = username;
			this.password = password;
		}
	}
}