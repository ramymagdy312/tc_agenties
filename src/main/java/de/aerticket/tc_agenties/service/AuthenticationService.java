package de.aerticket.tc_agenties.service;

import de.aerticket.tc_agenties.config.MicrositeConfig;
import de.aerticket.tc_agenties.constants.ApplicationConstants;
import de.aerticket.tc_agenties.entity.MicrositeMapping;
import de.aerticket.tc_agenties.exception.AuthenticationException;
import de.aerticket.tc_agenties.exception.JwtValidationException;
import de.aerticket.tc_agenties.model.AgencyStatus;
import de.aerticket.tc_agenties.model.AuthenticationResponse;
import de.aerticket.tc_agenties.model.CockpitAgency;
import de.aerticket.tc_agenties.model.JwtClaims;
import de.aerticket.tc_agenties.model.TCAgencydata;
import de.aerticket.tc_agenties.model.TravelcAgencyRequest;
import de.aerticket.tc_agenties.model.TravelcUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Authentication domain service.
 *
 * Responsibilities:
 * - Validate inputs and JWT
 * - Resolve microsite configuration
 * - Ensure agency and user exist/synchronized in TravelCompositor
 * - Build a consistent response for the caller
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

	private static final String DEFAULT_LANGUAGE = "DE";
	private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^[A-Za-z]{2}$");
	private static final Pattern TYPE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,30}$");

	private final JwtService jwtService;
	private final MicrositeMappingService micrositeMappingService;
	private final MicrositeConfig micrositeConfig;
	private final PasswordService passwordService;
	private final TravelCompositorService travelCompositorService;
	private final CockpitService cockpitService;

	/**
	 * Authenticate user and prepare redirect data to microsite.
	 */
	public AuthenticationResponse authenticateUser(String jwtToken, String language, String type) {
		validateInputToken(jwtToken);

		// Normalize simple inputs early to avoid spreading sanitation logic
		final String normalizedLanguage = normalizeLanguage(language);
		final String normalizedType = normalizeType(type);

		log.info("Authenticating user - lang={}, type={}", normalizedLanguage, normalizedType);

		// Parse and validate JWT token
		JwtClaims claims = parseAndValidateJwtToken(jwtToken);
		log.info("JWT validated - agent: {} {}, company: {}, agency: {}",
				claims.getAgentFirstName(), claims.getAgentLastName(), claims.getCompanyCode(),
				claims.getAgencyNumber());

		// Resolve microsite info
		MicrositeInfo micrositeInfo = resolveMicrositeInfo(claims.getCompanyCode());

		// Generate encrypted password
		String encryptedPassword = generateUserPassword(claims);

		// Ensure agency is active or try to sync from Cockpit
		AgencyStatus agencyStatus = ensureAgencyActive(claims.getAgencyNumber(), micrositeInfo);

		// Ensure user exists or try to create via Cockpit mapping
		boolean userExists = ensureUserExists(claims, micrositeInfo.microsite);

		// Build final redirect URL
		String returnUrl = buildReturnUrl(
				micrositeInfo.micrositeUrl,
				normalizedLanguage,
				normalizedType,
				claims.getJobId(),
				encryptedPassword,
				claims.getAgencyNumber());

		// Build response
		return buildSuccessResponse(
				claims,
				micrositeInfo,
				encryptedPassword,
				agencyStatus,
				userExists,
				normalizedLanguage,
				normalizedType,
				returnUrl);
	}

	// ---------- Public flow helpers ----------

	private void validateInputToken(String jwtToken) {
		if (!StringUtils.hasText(jwtToken)) {
			throw new IllegalArgumentException("JWT token cannot be empty");
		}
	}

	/**
	 * Parse and validate JWT token
	 */
	private JwtClaims parseAndValidateJwtToken(String jwtToken) {
		try {
			JwtClaims claims = jwtService.parseJwtToken(jwtToken);
			if (!jwtService.isTokenValid(claims)) {
				throw new JwtValidationException(ApplicationConstants.ErrorMessages.INVALID_JWT_TOKEN);
			}
			return claims;
		} catch (Exception e) {
			if (e instanceof JwtValidationException) {
				throw e;
			}
			throw new JwtValidationException("Failed to parse JWT token: " + e.getMessage(), e);
		}
	}

	/**
	 * Resolve microsite information based on company code.
	 */
	private MicrositeInfo resolveMicrositeInfo(String companyCode) {
		Optional<MicrositeMapping> micrositeMapping = micrositeMappingService
				.getMicrositeMappingByCompanyCode(companyCode);

		if (micrositeMapping.isEmpty()) {
			log.warn("No microsite mapping found for company code: {}, using fallback configuration", companyCode);
			return new MicrositeInfo(
					micrositeConfig.getFallbackUrl(),
					null,
					micrositeConfig.getFallbackMicrosite(),
					micrositeConfig.getFallbackMicrositeAPI());
		}

		MicrositeMapping mapping = micrositeMapping.get();
		log.info("Microsite mapping found for company {}: {}", companyCode, mapping.getMicrositeUrl());
		return new MicrositeInfo(
				mapping.getMicrositeUrl(),
				mapping.getName(),
				mapping.getMicrosite(),
				mapping.getMicrositeApi());
	}

	/**
	 * Generate encrypted password for user.
	 */
	private String generateUserPassword(JwtClaims claims) {
		String encryptedPassword = passwordService.generateEncryptedPassword(
				claims.getJobId(), claims.getAgencyNumber());

		if (encryptedPassword == null) {
			throw new AuthenticationException("Failed to generate user password");
		}

		log.debug("Generated encrypted password for user: {} {}",
				claims.getAgentFirstName(), claims.getAgentLastName());
		return encryptedPassword;
	}

	/**
	 * Ensure agency status is ACTIVE. If not, try to sync from Cockpit.
	 */
	private AgencyStatus ensureAgencyActive(String agencyNumber, MicrositeInfo micrositeInfo) {
		AgencyStatus currentStatus = travelCompositorService.checkAgencyStatus(micrositeInfo.micrositeApi,
				agencyNumber);

		if (currentStatus == AgencyStatus.ACTIVE) {
			log.debug("Agency {} is ACTIVE", agencyNumber);
			return currentStatus;
		}

		if (currentStatus == AgencyStatus.INACTIVE || currentStatus == AgencyStatus.NOT_FOUND) {
			log.debug("Agency {} is {}, attempting Cockpit sync", agencyNumber, currentStatus);
			boolean synced = syncAgencyFromCockpit(agencyNumber, micrositeInfo.microsite, currentStatus);
			if (synced) {
				log.info("Successfully synchronized agency {} from Cockpit", agencyNumber);
			} else {
				log.warn("Failed to synchronize agency {} from Cockpit", agencyNumber);
			}
		}

		return currentStatus;
	}

	/**
	 * Ensure user exists in TravelCompositor. If not, attempt to create using
	 * Cockpit data.
	 */
	private boolean ensureUserExists(JwtClaims claims, String microsite) {
		Boolean exists = travelCompositorService.getUser(microsite, claims.getAgencyNumber(), claims.getJobId());
		if (Boolean.TRUE.equals(exists)) {
			log.debug("User {} exists for agency {}", claims.getJobId(), claims.getAgencyNumber());
			return true;
		}

		log.debug("User {} for agency {} not found, attempting Cockpit sync", claims.getJobId(),
				claims.getAgencyNumber());

		CockpitAgency cockpitAgency = cockpitService.getAgency(claims.getAgencyNumber());
		TravelcUserRequest userRequest = cockpitService.convertToTravelcUserRequest(claims, cockpitAgency);
		if (userRequest != null) {
			boolean created = Boolean.TRUE.equals(travelCompositorService.createUser(userRequest, microsite));
			if (created) {
				log.info("Successfully synchronized user {} from Cockpit", claims.getJobId());
			} else {
				log.warn("Failed to create user {} in TravelCompositor", claims.getJobId());
			}
			return created;
		}

		log.warn("Failed to build TravelCompositor user from Cockpit for jobId {}", claims.getJobId());
		return false;
	}

	/**
	 * Synchronize agency data from Cockpit to TravelCompositor.
	 */
	private boolean syncAgencyFromCockpit(String agencyNumber, String microsite, AgencyStatus currentStatus) {
		try {
			CockpitAgency cockpitAgency = cockpitService.getAgency(agencyNumber);
			if (cockpitAgency == null) {
				log.warn("Agency {} not found in Cockpit", agencyNumber);
				return false;
			}

			// Convert Cockpit data to TravelCompositor format
			TravelcAgencyRequest tcRequest = cockpitService.convertToTravelcRequest(cockpitAgency);

			// Create or update depending on current status
			Boolean ok = (currentStatus == AgencyStatus.NOT_FOUND)
					? travelCompositorService.createAgency(tcRequest, microsite)
					: travelCompositorService.updateAgency(tcRequest, microsite);

			if (Boolean.TRUE.equals(ok)) {
				// Verify by fetching the just-created/updated agency
				TCAgencydata tcAgency = travelCompositorService.getAgencyData(microsite, agencyNumber);
				return tcAgency != null;
			}
			return false;
		} catch (Exception e) {
			log.error("Error syncing agency {} from Cockpit: {}", agencyNumber, e.getMessage());
			return false;
		}
	}

	/**
	 * Build the microsite return URL safely.
	 */
	private String buildReturnUrl(String baseMicrositeUrl,
			String language,
			String type,
			String user,
			String encryptedPassword,
			String agency) {
		return UriComponentsBuilder
				.fromHttpUrl(baseMicrositeUrl)
				.pathSegment(language, "home")
				.queryParam("tripType", type)
				.queryParam("submit", true)
				.queryParam("user", user)
				.queryParam("password", encryptedPassword)
				.queryParam("agency", agency)
				.toUriString();
	}

	/**
	 * Build successful authentication response.
	 */
	private AuthenticationResponse buildSuccessResponse(
			JwtClaims claims,
			MicrositeInfo micrositeInfo,
			String encryptedPassword,
			AgencyStatus agencyStatus,
			boolean userIsAvailable,
			String language,
			String type,
			String returnUrl) {

		return AuthenticationResponse.builder()
				.success(true)
				.message(ApplicationConstants.SuccessMessages.AUTHENTICATION_SUCCESSFUL)
				.agentFirstName(claims.getAgentFirstName())
				.agentLastName(claims.getAgentLastName())
				.agencyNumber(claims.getAgencyNumber())
				.companyCode(claims.getCompanyCode())
				.role(claims.getRole())
				.jobId(claims.getJobId())
				.micrositeUrl(returnUrl)
				.micrositeName(micrositeInfo.micrositeName)
				.microsite(micrositeInfo.microsite)
				.micrositeApi(micrositeInfo.micrositeApi)
				.encryptedPassword(encryptedPassword)
				.agencyStatus(agencyStatus.name())
				.agencyStatusDescription(agencyStatus.getDescription())
				.userStatus(userIsAvailable)
				.language(language)
				.type(type)
				.build();
	}

	// ---------- Small utilities ----------

	private String normalizeLanguage(String language) {
		if (!StringUtils.hasText(language)) {
			return DEFAULT_LANGUAGE;
		}
		String candidate = language.trim();
		if (candidate.length() > 2) {
			candidate = candidate.substring(0, 2);
		}
		candidate = candidate.toUpperCase(Locale.ROOT);
		return LANGUAGE_PATTERN.matcher(candidate).matches() ? candidate : DEFAULT_LANGUAGE;
	}

	private String normalizeType(String type) {
		if (!StringUtils.hasText(type)) {
			return ""; // keep empty if not provided to preserve original behavior
		}
		String candidate = type.trim();
		if (!TYPE_PATTERN.matcher(candidate).matches()) {
			// Fallback to a conservative variant (alphanumeric only)
			candidate = candidate.replaceAll("[^A-Za-z0-9]", "");
		}
		return candidate;
	}

	/**
	 * Inner value object to hold microsite information.
	 */
	private static class MicrositeInfo {
		final String micrositeUrl;
		final String micrositeName;
		final String microsite;
		final String micrositeApi;

		MicrositeInfo(String micrositeUrl, String micrositeName, String microsite, String micrositeApi) {
			this.micrositeUrl = micrositeUrl;
			this.micrositeName = micrositeName;
			this.microsite = microsite;
			this.micrositeApi = micrositeApi;
		}
	}
}