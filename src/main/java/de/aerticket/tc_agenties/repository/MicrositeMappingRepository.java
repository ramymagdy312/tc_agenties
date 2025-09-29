package de.aerticket.tc_agenties.repository;

import de.aerticket.tc_agenties.entity.MicrositeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MicrositeMappingRepository extends JpaRepository<MicrositeMapping, Long> {

    /**
     * Find microsite mapping by company code
     */
    List<MicrositeMapping> findByCompanyCode(String companyCode);

    /**
     * Find first microsite mapping by company code (if there's only one expected)
     */
    Optional<MicrositeMapping> findFirstByCompanyCode(String companyCode);

    /**
     * Custom query to get microsite URL by company code
     */
    @Query("SELECT m.micrositeUrl FROM MicrositeMapping m WHERE m.companyCode = :companyCode")
    Optional<String> findMicrositeUrlByCompanyCode(@Param("companyCode") String companyCode);

    /**
     * Check if company code exists
     */
    boolean existsByCompanyCode(String companyCode);
}