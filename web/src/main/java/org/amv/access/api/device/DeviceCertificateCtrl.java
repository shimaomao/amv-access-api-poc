package org.amv.access.api.device;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.auth.ApplicationAuthentication;
import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.client.model.CreateDeviceCertificateRequestDto;
import org.amv.access.client.model.CreateDeviceCertificateResponseDto;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

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
            @ApiResponse(code = HttpStatus.CREATED_201, message = "Created", response = CreateDeviceCertificateResponseDto.class)
    })
    @PostMapping
    public DeferredResult<ResponseEntity<CreateDeviceCertificateResponseDto>> createDeviceCertificate(
            ApplicationAuthentication auth,
            @RequestBody @Valid CreateDeviceCertificateRequestDto requestBody) {
        requireNonNull(auth);
        requireNonNull(requestBody);

        DeferredResult<ResponseEntity<CreateDeviceCertificateResponseDto>> deferred = new DeferredResult<>();

        Consumer<ResponseEntity<CreateDeviceCertificateResponseDto>> onNext = result -> {
            boolean canBeSent = !deferred.isSetOrExpired();
            if (!canBeSent) {
                log.error("Deferred result is already set or expired: {}", result);
            } else {
                deferred.setResult(result);
            }
        };

        Consumer<Throwable> onError = e -> {
            log.error("", e);
            deferred.setErrorResult(e);
        };

        CreateDeviceCertificateRequest createDeviceCertificateRequest = CreateDeviceCertificateRequest.builder()
                .appId(auth.getAppId())
                .publicKey(requestBody.getDevicePublicKey())
                .name(RandomStringUtils.randomAlphabetic(16))
                .build();

        deviceCertificateService.createDeviceCertificate(createDeviceCertificateRequest)
                .map(deviceCertificateEntity -> CreateDeviceCertificateResponseDto.builder()
                        .deviceCertificate(deviceCertificateEntity.getSignedCertificateBase64())
                        .issuerPublicKey(deviceCertificateEntity.getIssuerPublicKeyBase64())
                        .build())
                .map(body -> ResponseEntity.status(HttpStatus.CREATED_201).body(body))
                .subscribeOn(Schedulers.parallel())
                .subscribe(onNext, onError);

        return deferred;
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
