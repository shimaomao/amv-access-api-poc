package org.amv.access.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ErrorResponseDto {

    @ApiModelProperty("List of errors")
    @JsonProperty("errors")
    @Singular("addError")
    private List<ErrorInfoDto> errors;

    @Value
    @Builder
    public static class ErrorInfoDto {
        @ApiModelProperty("Reason of error")
        @JsonProperty("title")
        private String title;

        @ApiModelProperty("Optional value which points to the field that causes this error. Ex. 'public_key'")
        @JsonProperty("source")
        private String source;

        @ApiModelProperty("Optional value of error details")
        @JsonProperty("detail")
        private String detail;
    }
}
