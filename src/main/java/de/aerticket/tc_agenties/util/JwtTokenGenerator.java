package de.aerticket.tc_agenties.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenGenerator {

    private static final String SECRET = "hkjvBw3h0kHaTP8zBTZQRrfF7peGrYcJkRNVQokBrPSutNIP0wwHwDlcgPCxyOd1";

    public static void main(String[] args) {
        JwtTokenGenerator generator = new JwtTokenGenerator();
        String token = generator.generateToken();
        System.out.println("Generated JWT: " + token);
    }
    
    public String generateToken() {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (60 * 1000);

        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("kid", "aer360")
                .setAudience("cockpit.aerticket.fr")
                .setExpiration(new Date(expMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
