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
public class TravelcUserRequest {

    private String username;

    private String password;

    private String name;

    private String surname;

    private String[] roles;

    private String email;

    private String agency;

    private String active;
}