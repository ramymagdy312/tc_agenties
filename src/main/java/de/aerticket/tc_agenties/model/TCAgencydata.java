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
public class TCAgencydata {

	private String id;

	private String active;

	private String name;

	@JsonProperty("addressText")
	private String addressText;

	@JsonProperty("postalCode")
	private String postalCode;

	private String city;

	private String region;

	private String country;

	@JsonProperty("phoneNumber")
	private String phoneNumber;

	@JsonProperty("documentNumber")
	private String documentNumber;

	@JsonProperty("contactPersonName")
	private String contactPersonName;

	@JsonProperty("contactPersonLastName")
	private String contactPersonLastName;

	private String email;

	@JsonProperty("businessName")
	private String businessName;

	@JsonProperty("billingeEmail")
	private String billingeEmail;

	private String taxes;

	@JsonProperty("invoiceType")
	private String invoiceType;

	@JsonProperty("managementGroup")
	private String managementGroup;

	@JsonProperty("deferredPaymentAllowed")
	private String deferredPaymentAllowed;

	@JsonProperty("deferredDays")
	private Integer deferredDays;

	@JsonProperty("deferredLimit")
	private Long deferredLimit;

	/**
	 * Check if agency is active
	 */
	public boolean isActive() {
		return "true".equalsIgnoreCase(active);
	}
}