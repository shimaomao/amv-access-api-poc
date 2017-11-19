package org.amv.access.api.access;

import io.prometheus.client.spring.web.PrometheusTimeMethod;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.certificate.AccessCertificateService;
import org.amv.access.certificate.AccessCertificateService.GetAccessCertificateContext;
import org.amv.access.certificate.AccessCertificateService.RevokeAccessCertificateContext;
import org.amv.access.api.access.model.CreateAccessCertificateRequestDto;
import org.amv.access.api.access.model.CreateAccessCertificateResponseDto;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.MoreHttpHeaders;
import org.amv.access.client.model.AccessCertificateDto;
import org.amv.access.client.model.ErrorResponseDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.amv.access.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
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

    @GetMapping
    @ApiOperation(
            value = "Retrieve all access certificates",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiImplicitParams({
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_NONCE, value = "Randomly generated nonce in base64", paramType = "header", required = true),
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_SIGNATURE, value = "Nonce signed with device private key in base64", paramType = "header", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = OK_200, message = "Return list of Access Certificates", response = GetAccessCertificatesResponseDto.class),
            @ApiResponse(code = UNAUTHORIZED_401, message = "If signature is invalid", response = ErrorResponseDto.class),
            @ApiResponse(code = NOT_FOUND_404, message = "If a device with given serial number is not found", response = ErrorResponseDto.class),
            @ApiResponse(code = UNPROCESSABLE_ENTITY_422, message = "if required params are missing or invalid", response = ErrorResponseDto.class)
    })
    @ResponseStatus(HttpStatus.OK)
    @PrometheusTimeMethod(name = "access_certificate_ctrl_retrieve_access_certificate", help = "")
    public ResponseEntity<GetAccessCertificatesResponseDto> getAccessCertificates(
            NonceAuthentication nonceAuthentication,
            @ApiParam(required = true) @PathVariable("deviceSerialNumber") String deviceSerialNumber) {
        requireNonNull(nonceAuthentication);
        requireNonNull(deviceSerialNumber);

        log.info("Fetch access certificates of device {}", deviceSerialNumber);

        GetAccessCertificateContext request = GetAccessCertificateContext.builder()
                .deviceSerialNumber(deviceSerialNumber.toLowerCase())
                .build();

        ResponseEntity<GetAccessCertificatesResponseDto> response = accessCertificateService
                .getAccessCertificates(nonceAuthentication, request)
                .map(accessCertificate -> AccessCertificateDto.builder()
                        .id(accessCertificate.getUuid())
                        .name(accessCertificate.getVehicle().getName())
                        .deviceAccessCertificate(accessCertificate.getSignedDeviceAccessCertificateBase64())
                        .vehicleAccessCertificate(accessCertificate.getSignedVehicleAccessCertificateBase64())
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

    @DeleteMapping("/{accessCertificateId}")
    @ApiOperation(
            value = "Revoke an access certificate"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_NONCE, value = "Randomly generated nonce in base64", paramType = "header", required = true),
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_SIGNATURE, value = "Nonce signed with device private key in base64", paramType = "header", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = NO_CONTENT_204, message = "The resource was deleted successfully."),
            @ApiResponse(code = BAD_REQUEST_400, message = "If required params are missing or invalid.", response = ErrorResponseDto.class),
            @ApiResponse(code = NOT_FOUND_404, message = "If a device with given serial number or access certificate is not found.", response = ErrorResponseDto.class)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PrometheusTimeMethod(name = "access_certificate_ctrl_revoke_access_certificate", help = "")
    public ResponseEntity<Void> revokeAccessCertificate(
            NonceAuthentication nonceAuthentication,
            @ApiParam(required = true) @PathVariable("deviceSerialNumber") String deviceSerialNumber,
            @ApiParam(required = true) @PathVariable("accessCertificateId") UUID accessCertificateId) {
        requireNonNull(nonceAuthentication);
        requireNonNull(deviceSerialNumber);
        requireNonNull(accessCertificateId);

        log.info("Revoke access certificate {} of device {}", accessCertificateId.toString(), deviceSerialNumber);

        RevokeAccessCertificateContext revokeAccessCertificateContext = RevokeAccessCertificateContext.builder()
                .deviceSerialNumber(deviceSerialNumber.toLowerCase())
                .accessCertificateId(accessCertificateId)
                .build();

        ResponseEntity<Void> response = accessCertificateService
                .revokeAccessCertificate(nonceAuthentication, revokeAccessCertificateContext)
                .map(foo -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.noContent().build())
                .block();

        return response;
    }


    // TODO: this method must either only be called by authorized users or not callable via http at all
    // TODO: it currently just exists for testing purposes
    @PostMapping
    @ApiOperation(
            value = "Create an access certificate",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiResponses({
            @ApiResponse(code = OK_200, message = "Success", response = CreateAccessCertificateResponseDto.class)
    })
    @ResponseStatus(HttpStatus.OK)
    @PrometheusTimeMethod(name = "access_certificate_ctrl_create_access_certificate", help = "")
    public ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificate(
            @ApiParam(required = true) @PathVariable("deviceSerialNumber") String deviceSerialNumber,
            @ApiParam(required = true) @RequestBody CreateAccessCertificateRequestDto createAccessCertificateRequestDto) {
        requireNonNull(deviceSerialNumber);
        requireNonNull(createAccessCertificateRequestDto);

        log.info("Create access certificates with application {} for device {} and vehicle {}",
                createAccessCertificateRequestDto.getAppId(),
                createAccessCertificateRequestDto.getDeviceSerialNumber(),
                createAccessCertificateRequestDto.getVehicleSerialNumber());

        if (!deviceSerialNumber.equalsIgnoreCase(createAccessCertificateRequestDto.getDeviceSerialNumber())) {
            throw new BadRequestException("Device Serial Numbers do not match");
        }

        BindException errors = new BindException(createAccessCertificateRequestDto, "createAccessCertificateRequest");
        createAccessCertificateRequestValidator.validate(createAccessCertificateRequestDto, errors);
        if (errors.hasErrors()) {
            throw new BadRequestException(errors.getMessage());
        }

        ResponseEntity<CreateAccessCertificateResponseDto> response = accessCertificateService
                .createAccessCertificate(AccessCertificateService.CreateAccessCertificateContext.builder()
                        .appId(createAccessCertificateRequestDto.getAppId().toLowerCase())
                        .deviceSerialNumber(createAccessCertificateRequestDto.getDeviceSerialNumber().toLowerCase())
                        .vehicleSerialNumber(createAccessCertificateRequestDto.getVehicleSerialNumber().toLowerCase())
                        .validityStart(createAccessCertificateRequestDto.getValidityStart())
                        .validityEnd(createAccessCertificateRequestDto.getValidityEnd())
                        .build())
                .map(accessCertificate -> CreateAccessCertificateResponseDto.builder()
                        .accessCertificate(AccessCertificateDto.builder()
                                .deviceAccessCertificate(accessCertificate.getSignedDeviceAccessCertificateBase64())
                                .vehicleAccessCertificate(accessCertificate.getSignedVehicleAccessCertificateBase64())
                                .build())
                        .build())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()))
                .block();

        return response;
    }
}
