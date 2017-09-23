package org.amv.access.swagger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import springfox.documentation.service.ApiInfo;

@Data
@ConfigurationProperties("amv.swagger")
public class SwaggerProperties {
    private static final ApiInfo DEFAULT = ApiInfo.DEFAULT;

    private boolean enabled = true;
    private String version = DEFAULT.getVersion();
    private String title = DEFAULT.getTitle();
    private String description = DEFAULT.getDescription();
    private String termsOfServiceUrl = DEFAULT.getTermsOfServiceUrl();
    private String license = DEFAULT.getLicense();
    private String licenseUrl = DEFAULT.getLicenseUrl();
    private String contactName = DEFAULT.getContact().getName();
    private String contactUrl = DEFAULT.getContact().getUrl();
    private String contactEmail = DEFAULT.getContact().getEmail();
}