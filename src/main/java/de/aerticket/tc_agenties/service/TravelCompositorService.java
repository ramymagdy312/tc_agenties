package de.aerticket.tc_agenties.service;

import de.aerticket.tc_agenties.model.TCAgencydata;
import de.aerticket.tc_agenties.model.AgencyStatus;
import de.aerticket.tc_agenties.model.TravelcAgencyRequest;
import de.aerticket.tc_agenties.model.TravelcUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class TravelCompositorService {

    private final RestTemplate restTemplate;
    private final TravelcAuthManager authManager;

    private static final String TC_API_BASE_URL = "https://kombireisen.suntrips.de/resources";
    private static final String LANGUAGE = "DE";

    /**
     * Check agency status in TravelCompositor
     */
    public AgencyStatus checkAgencyStatus(String microsite, String agencyNumber) {
        try {
            log.info("Checking agency status for microsite: {} and agencyNumber: {}", microsite, agencyNumber);

            TCAgencydata agencyData = getAgencyData(microsite, agencyNumber);

            if (agencyData == null) {
                log.warn("Agency not found in TravelCompositor: microsite={}, agencyNumber={}",
                        microsite, agencyNumber);
                return AgencyStatus.NOT_FOUND;
            }

            if (agencyData.isActive()) {
                log.info("Agency is active in TravelCompositor: microsite={}, agencyNumber={}",
                        microsite, agencyNumber);
                return AgencyStatus.ACTIVE;
            } else {
                log.warn("Agency is inactive in TravelCompositor: microsite={}, agencyNumber={}",
                        microsite, agencyNumber);
                return AgencyStatus.INACTIVE;
            }

        } catch (Exception e) {
            log.error("Error checking agency status for microsite={}, agencyNumber={}: {}",
                    microsite, agencyNumber, e.getMessage());
            return AgencyStatus.ERROR;
        }
    }

    /**
     * Get agency data from TravelCompositor API
     */
    public TCAgencydata getAgencyData(String microsite, String agencyNumber) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TC_API_BASE_URL)
                    .pathSegment("agency", microsite, agencyNumber)
                    .queryParam("lang", LANGUAGE)
                    .toUriString();
            log.debug("Making API call to TravelCompositor: {}", url);

            HttpHeaders headers = createAuthHeaders(microsite);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<TCAgencydata> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, TCAgencydata.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Successfully retrieved agency data from TravelCompositor");
                return response.getBody();
            } else {
                log.warn("Empty response from TravelCompositor API");
                return null;
            }

        } catch (HttpClientErrorException.NotFound e) {
            log.info("Agency not found in TravelCompositor: microsite={}, agencyNumber={}",
                    microsite, agencyNumber);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("HTTP error calling TravelCompositor API: {} - {}",
                    e.getStatusCode(), e.getMessage());
            throw new RuntimeException("TravelCompositor API error: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            log.error("Network error calling TravelCompositor API: {}", e.getMessage());
            throw new RuntimeException("Network error accessing TravelCompositor: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling TravelCompositor API: {}", e.getMessage());
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Create new agency in TravelCompositor
     */
    public Boolean createAgency(TravelcAgencyRequest agency, String microsite) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TC_API_BASE_URL)
                    .pathSegment("agency", microsite, "")
                    .queryParam("plainfees", true)
                    .toUriString();
            log.info("Creating agency in TravelCompositor: microsite={}, agencyId={}", microsite,
                    agency.getExternalId());

            HttpHeaders headers = createAuthHeaders(microsite);
            HttpEntity<TravelcAgencyRequest> entity = new HttpEntity<>(agency, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            boolean success = response.getStatusCode() == HttpStatus.OK;
            log.info("Agency creation result: {}", success ? "SUCCESS" : "FAILED");
            return success;

        } catch (Exception e) {
            log.error("Error creating agency in TravelCompositor: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Update existing agency in TravelCompositor
     */
    public Boolean updateAgency(TravelcAgencyRequest agency, String microsite) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TC_API_BASE_URL)
                    .pathSegment("agency", microsite, "")
                    .queryParam("plainfees", true)
                    .toUriString();
            log.info("Updating agency in TravelCompositor: microsite={}, agencyId={}", microsite,
                    agency.getExternalId());

            HttpHeaders headers = createAuthHeaders(microsite);
            HttpEntity<TravelcAgencyRequest> entity = new HttpEntity<>(agency, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class);

            boolean success = response.getStatusCode() == HttpStatus.OK;
            log.info("Agency update result: {}", success ? "SUCCESS" : "FAILED");
            return success;

        } catch (Exception e) {
            log.error("Error updating agency in TravelCompositor: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if user exists in TravelCompositor
     */
    public Boolean getUser(String microsite, String agencyNumber, String userId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TC_API_BASE_URL)
                    .pathSegment("user", microsite, agencyNumber, userId)
                    .toUriString();
            log.debug("Checking user existence: microsite={}, agency={}, userId={}",
                    microsite, agencyNumber, userId);

            HttpHeaders headers = createAuthHeaders(microsite);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            boolean exists = response.getStatusCode() == HttpStatus.OK;
            log.debug("User exists: {}", exists);
            return exists;

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("User not found: microsite={}, agency={}, userId={}",
                    microsite, agencyNumber, userId);
            return false;
        } catch (Exception e) {
            log.error("Error checking user existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Create new user in TravelCompositor
     */
    public Boolean createUser(TravelcUserRequest user, String microsite) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TC_API_BASE_URL)
                    .pathSegment("user", microsite, user.getAgency())
                    .toUriString();
            log.info("Creating user in TravelCompositor: microsite={}, agency={}, username={}",
                    microsite, user.getAgency(), user.getUsername());

            HttpHeaders headers = createAuthHeaders(microsite);
            HttpEntity<TravelcUserRequest> entity = new HttpEntity<>(user, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            boolean success = response.getStatusCode() == HttpStatus.OK;
            log.info("User creation result: {}", success ? "SUCCESS" : "FAILED");
            return success;

        } catch (Exception e) {
            log.error("Error creating user in TravelCompositor: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Update existing user in TravelCompositor
     */
    public Boolean updateUser(TravelcUserRequest user, String microsite) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TC_API_BASE_URL)
                    .pathSegment("user", microsite, user.getAgency())
                    .toUriString();
            log.info("Updating user in TravelCompositor: microsite={}, agency={}, username={}",
                    microsite, user.getAgency(), user.getUsername());

            HttpHeaders headers = createAuthHeaders(microsite);
            HttpEntity<TravelcUserRequest> entity = new HttpEntity<>(user, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class);

            boolean success = response.getStatusCode() == HttpStatus.OK;
            log.info("User update result: {}", success ? "SUCCESS" : "FAILED");
            return success;

        } catch (Exception e) {
            log.error("Error updating user in TravelCompositor: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Create HTTP headers with authentication token
     */
    private HttpHeaders createAuthHeaders(String microsite) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("auth-token", authManager.getToken(microsite));
        return headers;
    }
}