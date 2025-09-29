package de.aerticket.tc_agenties.model;

public enum AgencyStatus {
    /**
     * Agency exists and is active in TravelCompositor
     */
    ACTIVE("Agency exists and is active"),

    /**
     * Agency exists but is inactive in TravelCompositor
     */
    INACTIVE("Agency exists but is inactive - needs update"),

    /**
     * Agency does not exist in TravelCompositor
     */
    NOT_FOUND("Agency not found in TravelCompositor"),

    /**
     * Error occurred while checking agency status
     */
    ERROR("Error occurred while checking agency status");

    private final String description;

    AgencyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}