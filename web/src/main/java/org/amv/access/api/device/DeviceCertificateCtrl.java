package org.amv.access.api.device;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.api.device.model.CreateDeviceCertificateResponse;
import org.amv.access.api.device.model.DeviceCertificateDto;
import org.amv.access.api.device.model.RevokeDeviceCertificateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@Slf4j
@RestController
@RequestMapping("/device_certificates")
public class DeviceCertificateCtrl {
    private final CreateDeviceCertificateRequestValidator createDeviceCertificateRequestValidator;
    private final DeviceCertificateService deviceCertificateService;

    @Autowired
    public DeviceCertificateCtrl(
            CreateDeviceCertificateRequestValidator createDeviceCertificateRequestValidator,
            DeviceCertificateService deviceCertificateService) {
        this.createDeviceCertificateRequestValidator = requireNonNull(createDeviceCertificateRequestValidator);
        this.deviceCertificateService = requireNonNull(deviceCertificateService);
    }

    @InitBinder
    public void setupBinder(WebDataBinder binder) {
        binder.addValidators(createDeviceCertificateRequestValidator);
    }

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = CreateDeviceCertificateResponse.class)
    })
    @PostMapping
    public DeferredResult<ResponseEntity<CreateDeviceCertificateResponse>> createDeviceCertificate(
            @Valid @RequestBody CreateDeviceCertificateRequest request) {
        requireNonNull(request);

        DeferredResult<ResponseEntity<CreateDeviceCertificateResponse>> deferred = new DeferredResult<>();

        Consumer<ResponseEntity<CreateDeviceCertificateResponse>> onNext = result -> {
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

        deviceCertificateService
                .createDeviceCertificate(request)
                .map(deviceCertificateEntity -> DeviceCertificateDto.builder()
                        .id(deviceCertificateEntity.getId())
                        .certificate(deviceCertificateEntity.getSignedCertificateBase64())
                        .appId(deviceCertificateEntity.getAppId())
                        .issuerName(deviceCertificateEntity.getIssuerName())
                        .issuerPublicKey(deviceCertificateEntity.getIssuerPublicKeyBase64())
                        .deviceName(deviceCertificateEntity.getDeviceName())
                        .deviceSerialNumber(deviceCertificateEntity.getDeviceSerialNumber())
                        .build())
                .map(deviceCertificateDto -> CreateDeviceCertificateResponse.builder()
                        .deviceCertificate(deviceCertificateDto)
                        .build())
                .map(ResponseEntity::ok)
                .subscribeOn(Schedulers.parallel())
                .subscribe(onNext, onError);

        return deferred;
    }

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
    }

}
