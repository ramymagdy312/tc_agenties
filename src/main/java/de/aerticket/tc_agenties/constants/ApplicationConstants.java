package de.aerticket.tc_agenties.constants;

/**
 * Application-wide constants
 */
public final class ApplicationConstants {

    private ApplicationConstants() {
        // Utility class - prevent instantiation
    }

    // API URLs
    public static final String TC_API_BASE_URL = "https://kombireisen.suntrips.de/resources";

    // Languages
    public static final String LANGUAGE_DE = "DE";
    public static final String LANGUAGE_EN = "EN";

    // Default values
    public static final String DEFAULT_LANGUAGE = LANGUAGE_EN;
    public static final String DEFAULT_TYPE = "SINGLE";

    // JWT Algorithm types
    public static final String JWT_ALGORITHM_ES256 = "ES256";
    public static final String JWT_ALGORITHM_HS256 = "HS256";

    // Error Messages
    public static final class ErrorMessages {
        public static final String INVALID_JWT_TOKEN = "Invalid or expired JWT token";
        public static final String AUTHENTICATION_FAILED = "Authentication failed";
        public static final String INTERNAL_SERVER_ERROR = "Internal server error";
        public static final String AGENCY_NOT_FOUND = "Agency not found";
        public static final String INVALID_INPUT_PARAMETERS = "Invalid input parameters";

        private ErrorMessages() {
        }
    }

    // Success Messages
    public static final class SuccessMessages {
        public static final String AUTHENTICATION_SUCCESSFUL = "Authentication successful";
        public static final String AGENCY_CREATED = "Agency created successfully";
        public static final String AGENCY_UPDATED = "Agency updated successfully";
        public static final String USER_CREATED = "User created successfully";
        public static final String USER_UPDATED = "User updated successfully";

        private SuccessMessages() {
        }
    }

}