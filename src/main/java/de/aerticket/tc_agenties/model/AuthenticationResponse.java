package de.aerticket.tc_agenties.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {
    private boolean success;
    private String message;
    private String agentFirstName;
    private String agentLastName;
    private String agencyNumber;
    private String companyCode;
    private String role;
    private String jobId;
    private String language;
    private String type;

    // Microsite information
    private String micrositeUrl;
    private String micrositeName;
    private String microsite;
    private String micrositeApi;

    // Generated password
    private String encryptedPassword;

    // Agency status in TravelCompositor
    private String agencyStatus;
    private String agencyStatusDescription;
    
    // User status in TravelCompositor
    private Boolean userStatus;
}