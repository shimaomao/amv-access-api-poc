package org.amv.access.api.access;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.ErrorResponseDto;
import org.amv.access.api.MoreHttpHeaders;
import org.amv.access.api.access.AccessCertificateService.GetAccessCertificateRequest;
import org.amv.access.api.access.AccessCertificateService.RevokeAccessCertificateRequest;
import org.amv.access.api.access.model.CreateAccessCertificateRequestDto;
import org.amv.access.api.access.model.CreateAccessCertificateResponseDto;
import org.amv.access.api.access.model.DeviceAndVehicleAccessCertificateDto;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.client.model.AccessCertificateDto;
import org.amv.access.client.model.GetAccessCertificatesResponseDto;
import org.amv.access.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    @ApiImplicitParams({
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_NONCE, value = "", paramType = "header"),
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_SIGNATURE, value = "", paramType = "header")
    })
    @ApiResponses(value = {
            @ApiResponse(code = OK_200, message = "Return list of Access Certificates", response = GetAccessCertificatesResponseDto.class),
            @ApiResponse(code = UNAUTHORIZED_401, message = "If signature is invalid", response = ErrorResponseDto.class),
            @ApiResponse(code = NOT_FOUND_404, message = "If a device with given serial number is not found", response = ErrorResponseDto.class),
            @ApiResponse(code = UNPROCESSABLE_ENTITY_422, message = "if required params are missing or invalid", response = ErrorResponseDto.class)
    })
    public ResponseEntity<GetAccessCertificatesResponseDto> getAccessCertificates(
            NonceAuthentication nonceAuthentication,
            @PathVariable("deviceSerialNumber") String deviceSerialNumber) {

        GetAccessCertificateRequest request = GetAccessCertificateRequest.builder()
                .deviceSerialNumber(deviceSerialNumber)
                .build();

        ResponseEntity<GetAccessCertificatesResponseDto> response = accessCertificateService
                .getAccessCertificates(nonceAuthentication, request)
                .map(accessCertificate -> AccessCertificateDto.builder()
                        .accessCertificate(accessCertificate.getSignedDeviceAccessCertificateBase64())
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
    @ApiImplicitParams({
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_NONCE, value = "", paramType = "header"),
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_SIGNATURE, value = "", paramType = "header")
    })
    @ApiResponses(value = {
            @ApiResponse(code = NO_CONTENT_204, message = "The resource was deleted successfully."),
            @ApiResponse(code = BAD_REQUEST_400, message = "If required params are missing or invalid.", response = ErrorResponseDto.class),
            @ApiResponse(code = NOT_FOUND_404, message = "If a device with given serial number or access certificate is not found.", response = ErrorResponseDto.class)
    })
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


    // TODO: this method must either only be called by authorized users or not callable via http at all
    // TODO: it currently just exists for testing purposes
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = CreateAccessCertificateResponseDto.class)
    })
    @PostMapping
    public ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificate(
            @PathVariable("deviceSerialNumber") String deviceSerialNumber,
            @RequestBody CreateAccessCertificateRequestDto createAccessCertificateRequest) {
        requireNonNull(deviceSerialNumber);
        requireNonNull(createAccessCertificateRequest);

        if (!deviceSerialNumber.equals(createAccessCertificateRequest.getDeviceSerialNumber())) {
            throw new BadRequestException("Device Serial Numbers do not match");
        }

        BindException errors = new BindException(createAccessCertificateRequest, "createAccessCertificateRequest");
        createAccessCertificateRequestValidator.validate(createAccessCertificateRequest, errors);
        if (errors.hasErrors()) {
            throw new BadRequestException(errors.getMessage());
        }

        ResponseEntity<CreateAccessCertificateResponseDto> response = accessCertificateService
                .createAccessCertificate(createAccessCertificateRequest)
                .map(accessCertificate -> CreateAccessCertificateResponseDto.builder()
                        .accessCertificate(DeviceAndVehicleAccessCertificateDto.builder()
                                .deviceAccessCertificate(AccessCertificateDto.builder()
                                        .accessCertificate(accessCertificate.getSignedDeviceAccessCertificateBase64())
                                        .build())
                                .vehicleAccessCertificate(AccessCertificateDto.builder()
                                        .accessCertificate(accessCertificate.getSignedVehicleAccessCertificateBase64())
                                        .build())
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
