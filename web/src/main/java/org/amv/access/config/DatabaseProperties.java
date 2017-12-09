package org.amv.access.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("amv.access.database")
public class DatabaseProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private String poolName;
    private int requiredSchemaVersion;
    private int maximumPoolSize = 25;
    private int idleTimeout = 30000;
    private String connectionTestQuery = "SELECT 1";
    private String columnEncryptionKey = "MySuperSecretKey";
}
