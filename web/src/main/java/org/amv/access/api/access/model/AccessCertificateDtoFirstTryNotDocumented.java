package org.amv.access.api.access.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
@ApiModel(description = "A resource representing an access certificate.")
public class AccessCertificateDtoFirstTryNotDocumented {
    @JsonProperty(value = "access_gaining_serial_number")
    @ApiModelProperty(notes = "either the vehicle or the device", dataType = "String (9 bytes)")
    private String accessGainingSerialNumber;

    @JsonProperty(value = "access_gaining_public_key")
    @ApiModelProperty(notes = "either the vehicle or the device", dataType = "String (64 bytes)")
    private String accessGainingPublicKey;

    @JsonProperty(value = "access_providing_serial_number")
    @ApiModelProperty(notes = "either the vehicle or the device", dataType = "String (9 bytes)")
    private String accessProvidingSerialNumber;

    @JsonProperty(value = "validity_start_year")
    @ApiModelProperty(notes = "00-99 decimal value, equals to 2000-2099", allowableValues = "0-99")
    private Integer validityStartYear;

    @JsonProperty(value = "validity_start_month")
    @ApiModelProperty(notes = "start month of validity", allowableValues = "1-12")
    private Integer validityStartMonth;

    @JsonProperty(value = "validity_start_day")
    @ApiModelProperty(notes = "start day of validity", allowableValues = "1-31")
    private Integer validityStartDay;

    @JsonProperty(value = "validity_start_hours")
    @ApiModelProperty(notes = "start hours of validity", allowableValues = "0-23")
    private Integer validityStartHours;

    @JsonProperty(value = "validity_start_minutes")
    @ApiModelProperty(notes = "start minutes of validity", allowableValues = "0-59")
    private Integer validityStartMinutes;

    @JsonProperty(value = "validity_end_year")
    @ApiModelProperty(notes = "end year of validity. 00-99 decimal value, equals to 2000-2099", allowableValues = "0-99")
    private Integer validityEndYear;

    @JsonProperty(value = "validity_end_month")
    @ApiModelProperty(notes = "end month of validity", allowableValues = "1-12")
    private Integer validityEndMonth;

    @JsonProperty(value = "validity_end_day")
    @ApiModelProperty(notes = "end day of validity", allowableValues = "1-31")
    private Integer validityEndDay;

    @JsonProperty(value = "validity_end_hours")
    @ApiModelProperty(notes = "end hours of validity", allowableValues = "0-23")
    private Integer validityEndHours;

    @JsonProperty(value = "validity_end_minutes")
    @ApiModelProperty(notes = "end minutes of validity", allowableValues = "0-59")
    private Integer validityEndMinutes;

    @JsonProperty(value = "permissions_size")
    private Integer permissionsSize;

    // x bytes data field 0-16 bytes
    @JsonProperty(value = "permissions")
    private String permissions;

    @JsonProperty(value = "signature")
    @ApiModelProperty(notes = "the signature of the Certificate Authority")
    private String signature;
}
