package de.aerticket.tc_agenties.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {
    private String sub;
    private String agencyNumber;
    private String companyCode;
    private String jobId;
    private String role;
    private String iss;
    private String aud;
    private Long exp;
    private String jti;
    private String agentFirstName;
    private String agentLastName;
    private Long iat;
    private Long nbf;
}