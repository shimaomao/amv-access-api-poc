package org.amv.access.api.access;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.access.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@Slf4j
@RestController
@RequestMapping("/access_certificates")
public class AccessCertificateCtrl {

    private final CreateAccessCertificateRequestValidator createAccessCertificateRequestValidator;
    private final AccessCertificateService accessCertificateService;

    @Autowired
    public AccessCertificateCtrl(
            CreateAccessCertificateRequestValidator createAccessCertificateRequestValidator,
            AccessCertificateService accessCertificateService) {
        this.createAccessCertificateRequestValidator = requireNonNull(createAccessCertificateRequestValidator);
        this.accessCertificateService = requireNonNull(accessCertificateService);
    }

    @InitBinder("createAccessCertificateRequest")
    public void setupBinder(WebDataBinder binder) {
        binder.addValidators(createAccessCertificateRequestValidator);
    }

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = GetAccessCertificateResponse.class)
    })
    @GetMapping
    public DeferredResult<ResponseEntity<GetAccessCertificateResponse>> getAccessCertificates(
            GetAccessCertificateRequest request) {

        DeferredResult<ResponseEntity<GetAccessCertificateResponse>> deferred = new DeferredResult<>();

        Consumer<ResponseEntity<GetAccessCertificateResponse>> onNext = result -> {
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

        // TODO: do not just take first one -> return a resources representing a list of certs
        accessCertificateService.getAccessCertificates(request)
                .map(accessCertificate -> AccessCertificateDto.builder()
                        .vehicleAccessCertificate(accessCertificate.getSignedVehicleCertificateBase64())
                        .deviceAccessCertificate(accessCertificate.getSignedDeviceCertificateBase64())
                        .build())
                .collectList()
                .map(list -> list.stream().findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(accessCertificateDto -> GetAccessCertificateResponse.builder()
                        .accessCertificate(accessCertificateDto)
                        .build())
                .map(ResponseEntity::ok)
                .otherwiseIfEmpty(Mono.fromCallable(() -> ResponseEntity.notFound().build()))
                .subscribeOn(Schedulers.parallel())
                .subscribe(onNext, onError);

        return deferred;
    }

    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = CreateAccessCertificateResponse.class)
    })
    @PostMapping
    public DeferredResult<ResponseEntity<CreateAccessCertificateResponse>> createAccessCertificate(
            @Valid @RequestBody CreateAccessCertificateRequest createAccessCertificateRequest) {
        requireNonNull(createAccessCertificateRequest);

        DeferredResult<ResponseEntity<CreateAccessCertificateResponse>> deferred = new DeferredResult<>();

        Consumer<ResponseEntity<CreateAccessCertificateResponse>> onNext = result -> {
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

        accessCertificateService.createAccessCertificate(createAccessCertificateRequest)
                .map(accessCertificate -> AccessCertificateDto.builder()
                        .vehicleAccessCertificate(accessCertificate.getSignedVehicleCertificateBase64())
                        .deviceAccessCertificate(accessCertificate.getSignedDeviceCertificateBase64())
                        .build())
                .map(accessCertificateDto -> CreateAccessCertificateResponse.builder()
                        .accessCertificate(accessCertificateDto)
                        .build())
                .map(ResponseEntity::ok)
                .otherwiseIfEmpty(Mono.fromCallable(() -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()))
                .subscribeOn(Schedulers.parallel())
                .subscribe(onNext, onError);

        return deferred;
    }

    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "No Content", response = Void.class)
    })
    @DeleteMapping
    public DeferredResult<ResponseEntity<Void>> revokeAccessCertificate(
            @RequestBody RevokeAccessCertificateRequest request) {
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

        accessCertificateService.revokeAccessCertificate(request)
                .map(foo -> ResponseEntity.noContent().<Void>build())
                .otherwiseIfEmpty(Mono.fromCallable(() -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()))
                .subscribeOn(Schedulers.parallel())
                .subscribe(onNext, onError);

        return deferred;
    }

}
