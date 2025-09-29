package de.aerticket.tc_agenties.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "aer_cockpit_mapping_microsite", schema = "lmxdb")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MicrositeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code", nullable = false)
    private String companyCode;

    @Column(name = "name")
    private String name;

    @Column(name = "microsite")
    private String microsite;

    @Column(name = "microsite_api")
    private String micrositeApi;

    @Column(name = "microsite_url")
    private String micrositeUrl;
}