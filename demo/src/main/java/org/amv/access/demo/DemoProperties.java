package org.amv.access.demo;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Optional;

@Data
@ConfigurationProperties("amv.access.demo")
public class DemoProperties {
    private boolean enabled = false;
    private DemoIssuer issuer;
    private List<DemoVehicle> vehicles = Lists.newArrayList();
    private List<DemoApplication> applications = Lists.newArrayList();

    public Optional<DemoIssuer> getIssuer() {
        return Optional.ofNullable(issuer);
    }

    @Data
    @Builder
    public static class DemoIssuer {
        private String name;
        private String publicKeyBase64;
        private String privateKeyBase64;

        @Tolerate
        public DemoIssuer() {
        }
    }

    @Data
    @Builder
    public static class DemoVehicle {
        private String name;
        private String serialNumber;
        private String publicKeyBase64;

        @Tolerate
        public DemoVehicle() {
        }
    }

    @Data
    @Builder
    public static class DemoApplication {
        private String name;
        private String appId;
        private String apiKey;

        @Tolerate
        public DemoApplication() {
        }
    }
}
