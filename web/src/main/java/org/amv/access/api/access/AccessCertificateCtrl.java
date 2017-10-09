package org.amv.access.api.access;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.ErrorResponseDto;
import org.amv.access.api.access.model.GetAccessCertificateRequest;
import org.amv.access.api.access.model.RevokeAccessCertificateRequest;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.model.AccessCertificateDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jetty.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/device/{deviceSerialNumber}/access_certificates")
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
            @ApiResponse(code = OK_200, message = "Return list of Access Certificates", response = GetAccessCertificatesResponseDto.class),
            @ApiResponse(code = UNAUTHORIZED_401, message = "If signature is invalid", response = ErrorResponseDto.class),
            @ApiResponse(code = NOT_FOUND_404, message = "If a device with given serial number is not found", response = ErrorResponseDto.class),
            @ApiResponse(code = UNPROCESSABLE_ENTITY_422, message = "if required params are missing or invalid", response = ErrorResponseDto.class)
    })
    @GetMapping
    public ResponseEntity<GetAccessCertificatesResponseDto> getAccessCertificates(
            NonceAuthentication nonceAuthentication,
            @PathVariable("deviceSerialNumber") String deviceSerialNumber) {

        GetAccessCertificateRequest request = GetAccessCertificateRequest.builder()
                .deviceSerialNumber(deviceSerialNumber)
                .build();

        ResponseEntity<GetAccessCertificatesResponseDto> response = accessCertificateService.getAccessCertificates(nonceAuthentication, request)
                .map(accessCertificate -> AccessCertificateDto.builder()
                        .accessCertificate(accessCertificate.getSignedDeviceCertificateBase64())
                        .build())
                .collectList()
                .map(accessCertificateDto -> GetAccessCertificatesResponseDto.builder()
                        .accessCertificates(accessCertificateDto)
                        .build())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity.notFound().build()))
                .block();

        return response;
    }

    /*
    // TODO: this method must either only be called by authorized users or not callable via http at all
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
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()))
                .subscribeOn(Schedulers.parallel())
                .subscribe(onNext, onError);

        return deferred;
    }*/

    @ApiResponses(value = {
            @ApiResponse(code = NO_CONTENT_204, message = "The resource was deleted successfully."),
            @ApiResponse(code = BAD_REQUEST_400, message = "If required params are missing or invalid.", response = ErrorResponseDto.class),
            @ApiResponse(code = NOT_FOUND_404, message = "If a device with given serial number or access certificate is not found.", response = ErrorResponseDto.class)
    })
    @DeleteMapping("/{accessCertificateId}")
    public ResponseEntity<Void> revokeAccessCertificate(
            NonceAuthentication nonceAuthentication,
            @PathVariable("deviceSerialNumber") String deviceSerialNumber,
            @PathVariable("accessCertificateId") UUID accessCertificateId) {
        requireNonNull(deviceSerialNumber);
        requireNonNull(accessCertificateId);

        RevokeAccessCertificateRequest revokeAccessCertificateRequest = RevokeAccessCertificateRequest.builder()
                .deviceSerialNumber(deviceSerialNumber)
                .accessCertificateId(accessCertificateId)
                .build();

        ResponseEntity<Void> response = accessCertificateService
                .revokeAccessCertificate(nonceAuthentication, revokeAccessCertificateRequest)
                .map(foo -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .block();

        return response;
    }

}
