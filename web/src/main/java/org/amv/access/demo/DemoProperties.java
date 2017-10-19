package org.amv.access.demo;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties("amv.access.demo")
public class DemoProperties {
    private boolean enabled = true;
    private List<DemoVehicle> vehicles = Lists.newArrayList();
    private List<DemoApplication> applications = Lists.newArrayList();

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
