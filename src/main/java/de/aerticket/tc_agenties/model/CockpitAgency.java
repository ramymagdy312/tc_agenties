package de.aerticket.tc_agenties.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CockpitAgency {

    @JsonProperty("agencyNumber")
    private String agencyNumber;

    @JsonProperty("companyName")
    private String companyName;

    private String address;

    private String city;

    private String zip;

    private String country;

    private String email;

    private String phone;

    private String name;

    private String fax;

    @JsonProperty("taxNumber")
    private String taxNumber;

    private String iban;

    private String bic;

    @JsonProperty("valueAddedTaxId")
    private String valueAddedTaxId;

    @JsonProperty("noAdvertisingMails")
    private boolean noAdvertisingMails;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("collectionMethod")
    private String collectionMethod;

    @JsonProperty("companyShortCode")
    private String companyShortCode;

    private String chain;

    private Integer branch;
}