package de.aerticket.tc_agenties.service;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import de.aerticket.tc_agenties.config.JwtConfig;
import de.aerticket.tc_agenties.model.JwtClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT utilities: parsing, signature validation, and minimal claims checks.
 *
 * Notes:
 * - Currently supports ES256 (ECDSA with P-256 and SHA-256) only.
 * - Expiration (exp) validation remains disabled to preserve current behavior.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private static final String ALG_ES256 = "ES256";

    private final JwtConfig jwtConfig;
    private final HttpClientService httpClientService;

    // Simple cache for public keys by URL to avoid repeated network calls
    private final Map<String, String> publicKeyCache = new ConcurrentHashMap<>();

    /**
     * Parse and validate JWT token (ES256), then convert to JwtClaims.
     */
    public JwtClaims parseJwtToken(String token) {
        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT token format");
            }

            // Decode header and body
            JSONObject headerObject = decodeBase64ToJSON(tokenParts[0]);
            JSONObject bodyObject = decodeBase64ToJSON(tokenParts[1]);

            String algorithm = headerObject.optString("alg");
            String kid = headerObject.optString("kid", null);
            String iss = bodyObject.optString("iss", null);

            log.debug("JWT alg={}, kid={}, iss={} detected", algorithm, kid, iss);

            // Only ES256 is supported
            if (!ALG_ES256.equalsIgnoreCase(algorithm)) {
                log.warn("Unsupported JWT alg: {}", algorithm);
                throw new IllegalArgumentException("Unsupported JWT algorithm: " + algorithm);
            }

            // Resolve public key URL and fetch key
            String publicKeyUrl = determinePublicKeyUrl(iss, kid);
            if (publicKeyUrl == null) {
                log.error("Could not determine public key URL for issuer: {} and kid: {}", iss, kid);
                throw new IllegalArgumentException("Unable to resolve public key URL");
            }

            String publicKey = getPublicKey(publicKeyUrl);
            if (publicKey == null) {
                log.error("Failed to retrieve public key from: {}", publicKeyUrl);
                throw new IllegalArgumentException("Public key retrieval failed");
            }

            // Verify signature
            boolean tokenIsValid = validateES256JWT(token, publicKey);
            if (!tokenIsValid) {
                log.warn("ES256 JWT token validation failed");
                throw new IllegalArgumentException("Invalid JWT token");
            }

            log.info("ES256 JWT token validation successful");
            return convertJSONToJwtClaims(bodyObject);

        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * Retrieve public key with a small in-memory cache by URL.
     */
    private String getPublicKey(String url) {
        return publicKeyCache.computeIfAbsent(url, k -> {
            try {
                return httpClientService.getPublicKey(k);
            } catch (Exception e) {
                log.error("Error fetching public key from {}: {}", k, e.getMessage());
                return null;
            }
        });
    }

    /**
     * Validate ES256 JWT token signature with PEM-encoded public key.
     */
    private boolean validateES256JWT(String jwtToken, String publicKeyPEM) {
        try {
            // Clean the PEM format
            String cleanedPEM = publicKeyPEM
                    .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            // Decode the public key
            byte[] publicKeyBytes = Base64.getDecoder().decode(cleanedPEM);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            // Create verifier and validate
            JWSVerifier verifier = new ECDSAVerifier(publicKey);
            JWSObject jwsObject = JWSObject.parse(jwtToken);

            boolean isValid = jwsObject.verify(verifier);

            if (isValid) {
                // Optional expiration check (disabled to keep behavior)
                JWTClaimsSet claimsSet = JWTParser.parse(jwtToken).getJWTClaimsSet();
                Date expirationTime = claimsSet.getExpirationTime();
                // if (expirationTime != null && expirationTime.before(new Date())) {
                // log.warn("JWT token is expired");
                // return false;
                // }
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error validating ES256 JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Determine public key URL based on issuer.
     */
    private String determinePublicKeyUrl(String issuer, String kid) {
        if (issuer == null || kid == null) {
            return null;
        }

        if (issuer.startsWith("qa-")) {
            return jwtConfig.getQaPublicKeyUrl().replace("{kid}", kid);
        } else if (issuer.startsWith("stg-")) {
            return jwtConfig.getStgPublicKeyUrl().replace("{kid}", kid);
        } else {
            return jwtConfig.getProdPublicKeyUrl().replace("{kid}", kid);
        }
    }

    /**
     * Decode Base64URL string to JSON object.
     */
    private JSONObject decodeBase64ToJSON(String base64) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(base64);
        String decodedString = new String(decodedBytes);
        return new JSONObject(decodedString);
    }

    /**
     * Convert JSONObject to JwtClaims POJO.
     */
    private JwtClaims convertJSONToJwtClaims(JSONObject jsonObject) {
        JwtClaims jwtClaims = new JwtClaims();

        jwtClaims.setSub(jsonObject.optString("sub"));
        jwtClaims.setAgencyNumber(jsonObject.optString("agencyNumber"));
        jwtClaims.setCompanyCode(jsonObject.optString("companyCode"));

        // Handle jobId safely
        if (jsonObject.has("jobId")) {
            Object jobIdObj = jsonObject.get("jobId");
            jwtClaims.setJobId(jobIdObj.toString());
        }

        jwtClaims.setRole(jsonObject.optString("role"));
        jwtClaims.setIss(jsonObject.optString("iss"));
        jwtClaims.setAud(jsonObject.optString("aud"));
        jwtClaims.setJti(jsonObject.optString("jti"));
        jwtClaims.setAgentFirstName(jsonObject.optString("agentFirstName"));
        jwtClaims.setAgentLastName(jsonObject.optString("agentLastName"));

        // Handle timestamps
        if (jsonObject.has("exp")) {
            jwtClaims.setExp(jsonObject.optLong("exp"));
        }
        if (jsonObject.has("iat")) {
            jwtClaims.setIat(jsonObject.optLong("iat"));
        }
        if (jsonObject.has("nbf")) {
            jwtClaims.setNbf(jsonObject.optLong("nbf"));
        }

        return jwtClaims;
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(JwtClaims claims) {
        if (claims.getExp() == null) {
            return true;
        }
        return new Date().getTime() / 1000 > claims.getExp();
    }

    /**
     * Validate token claims logically (minimal checks to preserve behavior).
     */
    public boolean isTokenValid(JwtClaims claims) {
        if (claims == null) {
            return false;
        }

        // Expiration check intentionally disabled to preserve current behavior
        // if (isTokenExpired(claims)) {
        // log.warn("JWT token is expired");
        // return false;
        // }

        // Not-before check
        if (claims.getNbf() != null && new Date().getTime() / 1000 < claims.getNbf()) {
            log.warn("JWT token is not yet valid");
            return false;
        }

        return true;
    }
}