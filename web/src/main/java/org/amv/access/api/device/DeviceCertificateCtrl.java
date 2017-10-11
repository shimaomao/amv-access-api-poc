package org.amv.access.api.device;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.ErrorResponseDto;
import org.amv.access.api.device.DeviceCertificateService.CreateDeviceCertificateRequest;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.amv.access.client.model.DeviceCertificateDto;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jetty.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/device_certificates")
public class DeviceCertificateCtrl {
    private final DeviceCertificateService deviceCertificateService;

    @Autowired
    public DeviceCertificateCtrl(DeviceCertificateService deviceCertificateService) {
        this.deviceCertificateService = requireNonNull(deviceCertificateService);
    }

    @ApiResponses(value = {
            @ApiResponse(code = CREATED_201, message = "DeviceEntity Certificate object", response = CreateDeviceCertificateResponseDto.class),
            @ApiResponse(code = BAD_REQUEST_400, message = "If required params are missing or invalid", response = ErrorResponseDto.class),
            @ApiResponse(code = UNAUTHORIZED_401, message = "If api_key is invalid", response = ErrorResponseDto.class),
            @ApiResponse(code = UNPROCESSABLE_ENTITY_422, message = "If given input semantically erroneous", response = ErrorResponseDto.class)
    })
    @PostMapping
    public ResponseEntity<CreateDeviceCertificateResponseDto> createDeviceCertificate(
            ApplicationAuthentication auth,
            @RequestBody CreateDeviceCertificateRequestDto requestBody) {
        requireNonNull(auth);
        requireNonNull(requestBody);

        CreateDeviceCertificateRequest createDeviceCertificateRequest = CreateDeviceCertificateRequest.builder()
                .appId(auth.getApplication().getAppId())
                .devicePublicKeyBase64(requestBody.getDevicePublicKey())
                .deviceName(RandomStringUtils.randomAlphabetic(16))
                .build();

        ResponseEntity<CreateDeviceCertificateResponseDto> response = deviceCertificateService
                .createDeviceCertificate(auth, createDeviceCertificateRequest)
                .map(deviceCertificateEntity -> DeviceCertificateDto.builder()
                        .deviceCertificate(deviceCertificateEntity.getSignedCertificateBase64())
                        .issuerPublicKey(deviceCertificateEntity.getIssuer().getPublicKeyBase64())
                        .build())
                .map(deviceCertificateDto -> CreateDeviceCertificateResponseDto.builder()
                        .deviceCertificate(deviceCertificateDto)
                        .build())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED_201).body(body))
                .block();

        return response;
    }

    /*
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = CreateDeviceCertificateResponse.class)
    })
    @DeleteMapping
    public DeferredResult<ResponseEntity<Void>> revokeDeviceCertificate(
            @RequestBody RevokeDeviceCertificateRequest request) {
        requireNonNull(request);

        DeferredResult<ResponseEntity<Void>> deferred = new DeferredResult<>();

        Consumer<ResponseEntity<Void>> onNext = response -> {
            boolean canBeSent = !deferred.isSetOrExpired();
            if (!canBeSent) {
                log.error("Deferred result is already set or expired.");
            } else {
                deferred.setResult(response);
            }
        };

        Consumer<Throwable> onError = e -> {
            log.error("", e);
            deferred.setErrorResult(e);
        };

        deviceCertificateService.revokeDeviceCertificate(request)
                .map(foo -> ResponseEntity.noContent().<Void>build())
                .subscribeOn(Schedulers.parallel())
                .subscribe(onNext, onError);

        return deferred;
    }*/

}
