package org.amv.access.issuer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("amv.issuer")
public class CertificateIssuerProperties {
    private String name;
    private String publicKey;
    private String privateKey;
}
