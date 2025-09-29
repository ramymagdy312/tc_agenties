package de.aerticket.tc_agenties.service;

import de.aerticket.tc_agenties.entity.MicrositeMapping;
import de.aerticket.tc_agenties.repository.MicrositeMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MicrositeMappingService {

    private final MicrositeMappingRepository micrositeMappingRepository;

    /**
     * Get microsite URL by company code
     */
    public Optional<String> getMicrositeUrlByCompanyCode(String companyCode) {
        log.debug("Fetching microsite URL for company code: {}", companyCode);

        try {
            Optional<String> micrositeUrl = micrositeMappingRepository.findMicrositeUrlByCompanyCode(companyCode);

            if (micrositeUrl.isPresent()) {
                log.info("Found microsite URL for company code {}: {}", companyCode, micrositeUrl.get());
            } else {
                log.warn("No microsite URL found for company code: {}", companyCode);
            }

            return micrositeUrl;

        } catch (Exception e) {
            log.error("Error fetching microsite URL for company code {}: {}", companyCode, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get complete microsite mapping by company code
     */
    public Optional<MicrositeMapping> getMicrositeMappingByCompanyCode(String companyCode) {
        log.debug("Fetching microsite mapping for company code: {}", companyCode);

        try {
            Optional<MicrositeMapping> mapping = micrositeMappingRepository.findFirstByCompanyCode(companyCode);

            if (mapping.isPresent()) {
                log.info("Found microsite mapping for company code: {}", companyCode);
            } else {
                log.warn("No microsite mapping found for company code: {}", companyCode);
            }

            return mapping;

        } catch (Exception e) {
            log.error("Error fetching microsite mapping for company code {}: {}", companyCode, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get all microsite mappings for a company code (in case there are multiple)
     */
    public List<MicrositeMapping> getAllMicrositeMappingsByCompanyCode(String companyCode) {
        log.debug("Fetching all microsite mappings for company code: {}", companyCode);

        try {
            List<MicrositeMapping> mappings = micrositeMappingRepository.findByCompanyCode(companyCode);
            log.info("Found {} microsite mappings for company code: {}", mappings.size(), companyCode);
            return mappings;

        } catch (Exception e) {
            log.error("Error fetching microsite mappings for company code {}: {}", companyCode, e.getMessage());
            return List.of();
        }
    }

    /**
     * Check if company code has microsite mapping
     */
    public boolean hasCompanyCodeMapping(String companyCode) {
        log.debug("Checking if company code has mapping: {}", companyCode);

        try {
            boolean exists = micrositeMappingRepository.existsByCompanyCode(companyCode);
            log.debug("Company code {} mapping exists: {}", companyCode, exists);
            return exists;

        } catch (Exception e) {
            log.error("Error checking company code mapping for {}: {}", companyCode, e.getMessage());
            return false;
        }
    }
}