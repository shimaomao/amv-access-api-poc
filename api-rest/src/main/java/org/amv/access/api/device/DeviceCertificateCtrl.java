package org.amv.access.api.device;

import com.google.common.net.HttpHeaders;
import io.prometheus.client.spring.web.PrometheusTimeMethod;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.certificate.DeviceCertificateService;
import org.amv.access.certificate.DeviceCertificateService.CreateDeviceCertificateContext;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.amv.access.client.model.DeviceCertificateDto;
import org.amv.access.client.model.ErrorResponseDto;
import org.amv.access.demo.DemoService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jetty.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/device_certificates")
public class DeviceCertificateCtrl {

    private final DeviceCertificateService deviceCertificateService;
    private final DemoService demoService;

    @Autowired
    public DeviceCertificateCtrl(DeviceCertificateService deviceCertificateService,
                                 DemoService demoService) {
        this.deviceCertificateService = requireNonNull(deviceCertificateService);
        this.demoService = requireNonNull(demoService);
    }

    @PostMapping
    @ApiOperation(
            value = "Create device certificates",
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = HttpHeaders.AUTHORIZATION,
                    value = "The api key that was generated for client. This token is internally associated to an app_id",
                    paramType = "header",
                    required = true
            )
    })
    @ApiResponses({
            @ApiResponse(code = CREATED_201, message = "Device Certificate object", response = CreateDeviceCertificateResponseDto.class),
            @ApiResponse(code = BAD_REQUEST_400, message = "If required params are missing or invalid", response = ErrorResponseDto.class),
            @ApiResponse(code = UNAUTHORIZED_401, message = "If api_key is invalid", response = ErrorResponseDto.class),
            @ApiResponse(code = UNPROCESSABLE_ENTITY_422, message = "If given input semantically erroneous", response = ErrorResponseDto.class)
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PrometheusTimeMethod(name = "device_certificate_ctrl_create_device_certificate", help = "")
    public ResponseEntity<CreateDeviceCertificateResponseDto> createDeviceCertificate(
            ApplicationAuthentication auth,
            @ApiParam(required = true) @RequestBody CreateDeviceCertificateRequestDto requestBody) {
        requireNonNull(auth);
        requireNonNull(requestBody);
        requireNonNull(requestBody.getDevicePublicKey());

        CreateDeviceCertificateContext createDeviceCertificateContext = CreateDeviceCertificateContext.builder()
                .appId(auth.getApplication().getAppId().toHex())
                .devicePublicKeyBase64(requestBody.getDevicePublicKey())
                .deviceName(RandomStringUtils.randomAlphabetic(16))
                .build();

        log.info("Create device certificates with application {} for device {} (key: {})",
                createDeviceCertificateContext.getAppId(),
                createDeviceCertificateContext.getDeviceName(),
                createDeviceCertificateContext.getDevicePublicKeyBase64());

        ResponseEntity<CreateDeviceCertificateResponseDto> response = deviceCertificateService
                .createDeviceCertificate(auth, createDeviceCertificateContext)
                .doOnNext(deviceCertificate -> {
                    demoService.createDemoAccessCertificateIfNecessary(deviceCertificate).block();
                })
                .map(deviceCertificate -> DeviceCertificateDto.builder()
                        .deviceCertificate(deviceCertificate.getSignedDeviceCertificateBase64())
                        .issuerPublicKey(deviceCertificate.getIssuer().getPublicKey().toBase64())
                        .build())
                .map(deviceCertificateDto -> CreateDeviceCertificateResponseDto.builder()
                        .deviceCertificate(deviceCertificateDto)
                        .build())
                .map(body -> ResponseEntity.status(CREATED_201).body(body))
                .block();

        return response;
    }
}
