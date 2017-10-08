package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;


@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class AccessCertificateDto {

    @JsonProperty("id")
    @ApiModelProperty(notes = "Recommended to use an ID that is not incremental and not easy to guess", dataType = "String")
    private String id;

    @JsonProperty("access_certificate")
    @ApiModelProperty(notes = "the full access certificate for the vehicle in binary format", dataType = "String (encoded in base64)")
    private String accessCertificate;

    @JsonProperty("name")
    @ApiModelProperty(notes = "You may return car model or license plate number", dataType = "String")
    private String name;

    @Tolerate
    protected AccessCertificateDto() {

    }
}
