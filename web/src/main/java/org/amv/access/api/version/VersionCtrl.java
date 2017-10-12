package org.amv.access.api.version;

import io.swagger.annotations.Api;
import lombok.Builder;
import lombok.Value;
import org.amv.access.AmvAccessApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

@RestController
@Api(hidden = true)
@RequestMapping(value = "/generic/version")
public class VersionCtrl {

    private final Environment environment;

    @Autowired
    public VersionCtrl(Environment environment) {
        this.environment = requireNonNull(environment);
    }

    @GetMapping
    public ResponseEntity<VersionInfoDto> version() {
        return ResponseEntity.ok(VersionInfoDto.builder()
                .version(AmvAccessApplication.class.getPackage().getImplementationVersion())
                .profiles(Arrays.asList(environment.getActiveProfiles()))
                .build());
    }

    @Value
    @Builder
    public static class VersionInfoDto {
        private String version;
        private List<String> profiles;
    }
}
