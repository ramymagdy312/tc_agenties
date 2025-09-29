package de.aerticket.tc_agenties.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PasswordService {

    private static final String SECRET_SUFFIX = "_*seCrEt+";

    /**
     * Generate encrypted password using the formula:
     * jobId + "_" + agencyNumber + "_*seCrEt+"
     * Then MD5 hash the result
     */
    public String generateEncryptedPassword(String jobId, String agencyNumber) {
        try {
            // Trim and convert to string
            String jobIdStr = jobId != null ? jobId.toString().trim() : "";
            String agencyNumberStr = agencyNumber != null ? agencyNumber.trim() : "";

            // Build plain password
            String plainPassword = jobIdStr + "_" + agencyNumberStr + SECRET_SUFFIX;

            log.debug("Generating password for jobId: {} and agencyNumber: {}", jobId, agencyNumber);

            // Generate MD5 hash
            String encryptedPassword = DigestUtils.md5Hex(plainPassword);

            log.debug("Password generated successfully");

            return encryptedPassword;

        } catch (Exception e) {
            log.error("Error generating password for jobId: {} and agencyNumber: {}: {}",
                    jobId, agencyNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Validate if the provided password matches the expected encrypted password
     */
    public boolean validatePassword(String providedPassword, String jobId, String agencyNumber) {
        try {
            String expectedPassword = generateEncryptedPassword(jobId, agencyNumber);
            return expectedPassword != null && expectedPassword.equals(providedPassword);
        } catch (Exception e) {
            log.error("Error validating password: {}", e.getMessage());
            return false;
        }
    }
}