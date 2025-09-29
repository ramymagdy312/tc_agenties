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
public class TravelcAgencyRequest {

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("companyname")
    private String companyname;

    @JsonProperty("addressText")
    private String addressText;

    private String city;

    @JsonProperty("postalCode")
    private String postalCode;

    private String country;

    private String email;

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    private String taxes;

    private String active;

    @JsonProperty("documentNumber")
    private String documentNumber;

    @JsonProperty("contactPersonName")
    private String contactPersonName;

    @JsonProperty("contactPersonLastName")
    private String contactPersonLastName;

    @JsonProperty("businessName")
    private String businessName;

    @JsonProperty("invoiceType")
    private String invoiceType;

    @JsonProperty("BIC")
    private String BIC;

    @JsonProperty("IBAN")
    private String IBAN;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("collectionMethod")
    private String collectionMethod;

    @JsonProperty("companyShortCode")
    private String companyShortCode;

    private String chain;

    @JsonProperty("taxNumber")
    private String taxNumber;

    @JsonProperty("valueAddedTaxId")
    private String valueAddedTaxId;
}