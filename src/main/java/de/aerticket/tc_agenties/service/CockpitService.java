package de.aerticket.tc_agenties.service;

import de.aerticket.tc_agenties.model.CockpitAgency;
import de.aerticket.tc_agenties.model.JwtClaims;
import de.aerticket.tc_agenties.model.TravelcAgencyRequest;
import de.aerticket.tc_agenties.model.TravelcUserRequest;
import de.aerticket.tc_agenties.util.JwtTokenGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CockpitService {

	private final RestTemplate restTemplate;
    private final JwtTokenGenerator jwtTokenGenerator; 

	private static final String COCKPIT_API_BASE_URL = "https://cockpit.aerticket.fr/api/aer360/agencies/";

	/**
	 * Get agency data from Cockpit API
	 */
	public CockpitAgency getAgency(String agencyNumber) {
		if (agencyNumber == null || agencyNumber.trim().isEmpty()) {
			log.warn("Agency number is null or empty");
			return null;
		}

		try {
			String url = COCKPIT_API_BASE_URL + agencyNumber;
			log.debug("Making API call to Cockpit: {}", url);

			HttpHeaders headers = createCockpitHeaders();
			HttpEntity<?> entity = new HttpEntity<>(headers);

			ResponseEntity<CockpitAgency> response = restTemplate.exchange(url, HttpMethod.GET, entity,
					CockpitAgency.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				log.debug("Successfully retrieved agency data from Cockpit for agency: {}", agencyNumber);
				return response.getBody();
			} else {
				log.warn("Empty response from Cockpit API for agency: {}", agencyNumber);
				return null;
			}

		} catch (HttpClientErrorException.NotFound e) {
			log.info("Agency not found in Cockpit: agencyNumber={}", agencyNumber);
			return null;
		} catch (HttpClientErrorException e) {
			log.error("HTTP error calling Cockpit API for agency {}: {} - {}", agencyNumber, e.getStatusCode(),
					e.getMessage());
			throw new RuntimeException("Cockpit API error: " + e.getMessage(), e);
		} catch (ResourceAccessException e) {
			log.error("Network error calling Cockpit API for agency {}: {}", agencyNumber, e.getMessage());
			throw new RuntimeException("Network error accessing Cockpit: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error calling Cockpit API for agency {}: {}", agencyNumber, e.getMessage());
			throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
		}
	}

	/**
	 * Check if agency exists in Cockpit
	 */
	public boolean agencyExists(String agencyNumber) {
		try {
			CockpitAgency agency = getAgency(agencyNumber);
			return agency != null;
		} catch (Exception e) {
			log.error("Error checking if agency exists in Cockpit: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Create HTTP headers for Cockpit API calls
	 */
	private HttpHeaders createCockpitHeaders() {
		String token = jwtTokenGenerator.generateToken();
		
		log.info("token: " , token);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.set("accept", "application/json");
		headers.set("Authorization", "Bearer " + token);

		return headers;
	}

	/**
	 * Convert CockpitAgency to TravelcAgencyRequest for creating/updating in
	 * TravelCompositor
	 */
	public TravelcAgencyRequest convertToTravelcRequest(CockpitAgency cockpitAgency) {
		if (cockpitAgency == null) {
			return null;
		}

		return TravelcAgencyRequest.builder().externalId(cockpitAgency.getAgencyNumber())
				.companyname(cockpitAgency.getCompanyName()).addressText(cockpitAgency.getAddress())
				.city(cockpitAgency.getCity()).postalCode(cockpitAgency.getZip()).country(cockpitAgency.getCountry())
				.email(cockpitAgency.getEmail()).phoneNumber(cockpitAgency.getPhone())
				.taxNumber(cockpitAgency.getTaxNumber()).valueAddedTaxId(cockpitAgency.getValueAddedTaxId())
				.IBAN(cockpitAgency.getIban()).BIC(cockpitAgency.getBic()).bankName(cockpitAgency.getBankName())
				.collectionMethod(cockpitAgency.getCollectionMethod())
				.companyShortCode(cockpitAgency.getCompanyShortCode()).chain(cockpitAgency.getChain()).active("true") // Default
																														// to
																														// active
				.taxes("0") // Default tax setting
				.invoiceType("NET") // Default invoice type
				.documentNumber("-").contactPersonName("-").contactPersonLastName("-")
				.businessName("AERTiCKET Conso GmbH Grenzenlos Reisen").build();
	}

	/**
	 * Convert CockpitAgency to TravelcUserRequest for creating/updating in
	 * TravelCompositor
	 */
	public TravelcUserRequest convertToTravelcUserRequest(JwtClaims claims, CockpitAgency cockpitAgency) {
		if (cockpitAgency == null) {
			return null;
		}

		String[] roles = new String[] { "user", "agent" };
		return TravelcUserRequest.builder().username(claims.getJobId().toString()).name(claims.getAgentFirstName())
				.surname(claims.getAgentLastName()).email(cockpitAgency.getEmail())
				.agency(cockpitAgency.getAgencyNumber()).password("").active("true").roles(roles).build();
	}
}