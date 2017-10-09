package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.util.List;

@Data
@Setter(AccessLevel.PROTECTED)
@Builder(builderClassName = "Builder")
public class GetAccessCertificatesResponseDto {

    @JsonProperty("access_certificates")
    @ApiModelProperty(notes = "list of Access Certificates")
    private List<AccessCertificateDto> accessCertificates;

    @Tolerate
    protected GetAccessCertificatesResponseDto() {

    }
}
