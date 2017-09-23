package org.amv.access.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("app")
public class ApplicationProperties {
    private String name;
    private String description;
    private String version;
}
