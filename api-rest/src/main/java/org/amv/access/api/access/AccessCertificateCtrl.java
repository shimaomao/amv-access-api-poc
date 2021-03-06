package org.amv.access.api.access;

import io.prometheus.client.spring.web.PrometheusTimeMethod;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.DeviceNonceAuthenticationImpl;
import org.amv.access.auth.IssuerNonceAuthentication;
import org.amv.access.auth.IssuerNonceAuthenticationImpl;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.certificate.AccessCertificateService;
import org.amv.access.certificate.AccessCertificateService.CreateAccessCertificateContext;
import org.amv.access.certificate.AccessCertificateService.RevokeAccessCertificateContext;
import org.amv.access.certificate.AccessCertificateService.UpdateAccessCertificateSignaturesContext;
import org.amv.access.certificate.SignedAccessCertificateResource;
import org.amv.access.client.MoreHttpHeaders;
import org.amv.access.client.model.*;
import org.amv.access.client.model.CreateAccessCertificateResponseDto.AccessCertificateSigningRequestDto;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.Key;
import org.amv.access.core.SignedAccessCertificate;
import org.amv.access.core.impl.KeyImpl;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.IssuerWithKeys;
import org.amv.access.exception.AmvAccessRuntimeException;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.issuer.IssuerService;
import org.amv.access.model.IssuerEntity;
import org.amv.access.util.MoreBase64;
import org.amv.highmobility.cryptotool.CryptotoolImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.eclipse.jetty.http.HttpStatus.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class AccessCertificateCtrl {

    private final Environment environment;
    private final DemoService demoService;
    private final IssuerService issuerService;
    private final CreateAccessCertificateRequestValidator createAccessCertificateRequestValidator;
    private final AccessCertificateService accessCertificateService;

    @Autowired
    public AccessCertificateCtrl(
            Environment environment,
            DemoService demoService,
            IssuerService issuerService,
            CreateAccessCertificateRequestValidator createAccessCertificateRequestValidator,
            AccessCertificateService accessCertificateService) {
        this.environment = requireNonNull(environment);
        this.demoService = requireNonNull(demoService);
        this.issuerService = requireNonNull(issuerService);
        this.createAccessCertificateRequestValidator = requireNonNull(createAccessCertificateRequestValidator);
        this.accessCertificateService = requireNonNull(accessCertificateService);
    }

    @GetMapping("/device/{deviceSerialNumber}/access_certificates")
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

        DeviceNonceAuthenticationImpl deviceNonceAuth = DeviceNonceAuthenticationImpl.builder()
                .nonceAuthentication(nonceAuthentication)
                .deviceSerialNumber(deviceSerialNumber)
                .build();

        ResponseEntity<GetAccessCertificatesResponseDto> response = accessCertificateService
                .getAccessCertificates(deviceNonceAuth)
                .map(accessCertificateResource -> {
                    SignedAccessCertificate signedAccessCertificate = accessCertificateResource.getSignedAccessCertificate();
                    return AccessCertificateDto.builder()
                            .id(accessCertificateResource.getUuid().toString())
                            .name(accessCertificateResource.getName())
                            .deviceAccessCertificate(signedAccessCertificate.getSignedDeviceAccessCertificateBase64())
                            .vehicleAccessCertificate(signedAccessCertificate.getSignedVehicleAccessCertificateBase64())
                            .build();
                })
                .collectList()
                .switchIfEmpty(Mono.just(Collections.emptyList()))
                .map(accessCertificateDto -> GetAccessCertificatesResponseDto.builder()
                        .accessCertificates(accessCertificateDto)
                        .build())
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity.notFound().build()))
                .block();

        return response;
    }

    @DeleteMapping("/issuer/{issuerUuid}/access_certificates/{accessCertificateId}")
    @ApiOperation(
            value = "Revoke an access certificate"
    )
    @ApiImplicitParams({
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_NONCE, value = "Randomly generated nonce in base64", paramType = "header", required = true),
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_SIGNATURE, value = "Nonce signed with issuer private key in base64", paramType = "header", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = NO_CONTENT_204, message = "The resource was deleted successfully."),
            @ApiResponse(code = BAD_REQUEST_400, message = "If required params are missing or invalid.", response = ErrorResponseDto.class),
            @ApiResponse(code = NOT_FOUND_404, message = "If an issuer with given name or access certificate is not found.", response = ErrorResponseDto.class)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PrometheusTimeMethod(name = "access_certificate_ctrl_revoke_access_certificate", help = "")
    public ResponseEntity<Boolean> revokeAccessCertificate(
            NonceAuthentication nonceAuthentication,
            @ApiParam(required = true) @PathVariable("issuerUuid") String issuerUuid,
            @ApiParam(required = true) @PathVariable("accessCertificateId") UUID accessCertificateId) {
        requireNonNull(nonceAuthentication);
        requireNonNull(issuerUuid);
        requireNonNull(accessCertificateId);

        log.info("Revoke access certificate {} of issuer {}", accessCertificateId.toString(), issuerUuid);

        IssuerNonceAuthenticationImpl issuerNonceAuth = IssuerNonceAuthenticationImpl.builder()
                .nonceAuthentication(nonceAuthentication)
                .issuerUuid(issuerUuid)
                .build();

        RevokeAccessCertificateContext revokeAccessCertificateContext = RevokeAccessCertificateContext.builder()
                .accessCertificateId(accessCertificateId)
                .build();

        ResponseEntity<Boolean> response = accessCertificateService
                .revokeAccessCertificate(issuerNonceAuth, revokeAccessCertificateContext)
                .map(result -> ResponseEntity.status(HttpStatus.NO_CONTENT).body(result))
                .block();

        return response;
    }

    /**
     * Create an access certificate
     *
     * @param nonceAuthentication               The authentication object
     * @param issuerUuid                        The UUID of the issuer
     * @param createAccessCertificateRequestDto The payload
     * @return An access certificate
     */
    @PostMapping("/issuer/{issuerUuid}/access_certificates")
    @ApiOperation(
            value = "Create an access certificate",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiImplicitParams({
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_NONCE, value = "Randomly generated nonce in base64", paramType = "header", required = true),
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_SIGNATURE, value = "Nonce signed with issuer private key in base64", paramType = "header", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = OK_200, message = "Success", response = CreateAccessCertificateResponseDto.class)
    })
    @ResponseStatus(HttpStatus.OK)
    @PrometheusTimeMethod(name = "access_certificate_ctrl_create_access_certificate", help = "")
    public ResponseEntity<CreateAccessCertificateResponseDto> createAccessCertificate(
            NonceAuthentication nonceAuthentication,
            @ApiParam(required = true) @PathVariable("issuerUuid") String issuerUuid,
            @ApiParam(required = true) @RequestBody CreateAccessCertificateRequestDto createAccessCertificateRequestDto) {
        requireNonNull(issuerUuid);
        requireNonNull(createAccessCertificateRequestDto);

        log.info("Create access certificates with application {} for device {} and vehicle {}",
                createAccessCertificateRequestDto.getAppId(),
                createAccessCertificateRequestDto.getDeviceSerialNumber(),
                createAccessCertificateRequestDto.getVehicleSerialNumber());

        IssuerNonceAuthenticationImpl issuerNonceAuth = IssuerNonceAuthenticationImpl.builder()
                .nonceAuthentication(nonceAuthentication)
                .issuerUuid(issuerUuid)
                .build();

        BindException errors = new BindException(createAccessCertificateRequestDto, "createAccessCertificateRequest");
        createAccessCertificateRequestValidator.validate(createAccessCertificateRequestDto, errors);
        if (errors.hasErrors()) {
            throw new BadRequestException(errors.getMessage());
        }

        CreateAccessCertificateContext context = CreateAccessCertificateContext.builder()
                .appId(createAccessCertificateRequestDto.getAppId().toLowerCase())
                .deviceSerialNumber(createAccessCertificateRequestDto.getDeviceSerialNumber().toLowerCase())
                .vehicleSerialNumber(createAccessCertificateRequestDto.getVehicleSerialNumber().toLowerCase())
                .validityStart(createAccessCertificateRequestDto.getValidityStart())
                .validityEnd(createAccessCertificateRequestDto.getValidityEnd())
                .build();

        ResponseEntity<CreateAccessCertificateResponseDto> response = accessCertificateService
                .createAccessCertificate(issuerNonceAuth, context)
                .map(accessCertificateResource -> {
                    AccessCertificate accessCertificate = accessCertificateResource.getAccessCertificate();
                    return CreateAccessCertificateResponseDto.builder()
                            .accessCertificateSigningRequest(AccessCertificateSigningRequestDto.builder()
                                    .id(accessCertificateResource.getUuid().toString())
                                    .deviceAccessCertificate(accessCertificate.getDeviceAccessCertificateBase64())
                                    .vehicleAccessCertificate(accessCertificate.getVehicleAccessCertificateBase64())
                                    .build())
                            .build();
                })
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()))
                .block();

        return response;
    }

    /**
     * Add a signature to an access certificate.
     *
     * @param nonceAuthentication The authentication object
     * @param issuerUuid          The UUID of the issuer
     * @param accessCertificateId The UUID of the access certificate
     * @param request             The payload
     * @return An access certificate
     */
    @PutMapping("/issuer/{issuerUuid}/access_certificates/{accessCertificateId}/signature")
    @ApiOperation(
            value = "Add signatures to an access certificate",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiImplicitParams({
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_NONCE, value = "Randomly generated nonce in base64", paramType = "header", required = true),
            @ApiImplicitParam(name = MoreHttpHeaders.AMV_SIGNATURE, value = "Nonce signed with issuer private key in base64", paramType = "header", required = true)
    })
    @ApiResponses({
            @ApiResponse(code = OK_200, message = "Success", response = Boolean.class)
    })
    @ResponseStatus(HttpStatus.OK)
    @PrometheusTimeMethod(name = "access_certificate_ctrl_update_access_certificate_signatures", help = "")
    public ResponseEntity<Boolean> updateAccessCertificateSignatures(
            NonceAuthentication nonceAuthentication,
            @ApiParam(required = true) @PathVariable("issuerUuid") String issuerUuid,
            @ApiParam(required = true) @PathVariable("accessCertificateId") UUID accessCertificateId,
            @ApiParam(required = true) @RequestBody UpdateAccessCertificateRequestDto request) {
        requireNonNull(issuerUuid);
        requireNonNull(accessCertificateId);
        requireNonNull(request);

        IssuerNonceAuthentication issuerNonceAuthentication = IssuerNonceAuthenticationImpl.builder()
                .nonceAuthentication(nonceAuthentication)
                .issuerUuid(issuerUuid)
                .build();

        UpdateAccessCertificateSignaturesContext context = UpdateAccessCertificateSignaturesContext.builder()
                .accessCertificateId(accessCertificateId)
                .vehicleAccessCertificateSignatureBase64(request.getVehicleAccessCertificateSignatureBase64())
                .signedVehicleAccessCertificateBase64(request.getSignedVehicleAccessCertificateBase64())
                .deviceAccessCertificateSignatureBase64(request.getDeviceAccessCertificateSignatureBase64())
                .signedDeviceAccessCertificateBase64(request.getSignedDeviceAccessCertificateBase64())
                .build();

        if (log.isDebugEnabled()) {
            log.debug("PUT signatures to access certificate {} by issuer {}", accessCertificateId, issuerUuid);
        }

        ResponseEntity<Boolean> response = accessCertificateService
                .addAccessCertificateSignatures(issuerNonceAuthentication, context)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.fromCallable(() -> ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()))
                .block();

        return response;
    }

    /**
     * Create an access certificate without being authorized.
     * This method throws an error in production environments.
     *
     * @param request The payload
     * @return true if the certificate has been created successfully
     */

    @PostMapping("/demo/access_certificates")
    @ApiOperation(
            value = "Create an access certificate by providing an issuers private key - this endpoint is disabled in production mode.",
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    @ApiResponses({
            @ApiResponse(code = OK_200, message = "Success", response = Boolean.class)
    })
    @ResponseStatus(HttpStatus.OK)
    @PrometheusTimeMethod(name = "access_certificate_ctrl_create_access_certificate_demo", help = "")
    public ResponseEntity<Boolean> createAccessCertificateDemo(
            @ApiParam(required = true) @RequestBody DemoCreateAccessCertificateRequestDto request) {

        boolean enableUnauthorizedAccess = !environment.acceptsProfiles("production");
        if (!enableUnauthorizedAccess) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
        }

        requireNonNull(request);
        CreateAccessCertificateRequestDto createAccessCertificateRequestDto = requireNonNull(request.getRequest());

        log.info("Create demo access certificates with application {} for device {} and vehicle {}",
                createAccessCertificateRequestDto.getAppId(),
                createAccessCertificateRequestDto.getDeviceSerialNumber(),
                createAccessCertificateRequestDto.getVehicleSerialNumber());

        BindException errors = new BindException(createAccessCertificateRequestDto, "createAccessCertificateRequest");
        createAccessCertificateRequestValidator.validate(createAccessCertificateRequestDto, errors);
        if (errors.hasErrors()) {
            throw new BadRequestException(errors.getMessage());
        }

        IssuerEntity issuerEntity = issuerService.findIssuerByUuid(UUID.fromString(request.getIssuerId()))
                .orElseThrow(() -> new NotFoundException("IssuerEntity not found"));

        Key issuerPrivateKey = KeyImpl.fromBase64(request.getIssuerPrivateKeyBase64());
        IssuerNonceAuthentication issuerNonceAuthentication = demoService.createNonceAuthentication(IssuerWithKeys.builder()
                .privateKey(issuerPrivateKey)
                .publicKey(KeyImpl.fromBase64(issuerEntity.getPublicKeyBase64()))
                .issuer(issuerEntity)
                .build());

        CreateAccessCertificateContext createAccessCertificateContext = CreateAccessCertificateContext.builder()
                .appId(createAccessCertificateRequestDto.getAppId().toLowerCase())
                .deviceSerialNumber(createAccessCertificateRequestDto.getDeviceSerialNumber().toLowerCase())
                .vehicleSerialNumber(createAccessCertificateRequestDto.getVehicleSerialNumber().toLowerCase())
                .validityStart(createAccessCertificateRequestDto.getValidityStart())
                .validityEnd(createAccessCertificateRequestDto.getValidityEnd())
                .build();

        SignedAccessCertificateResource signedAccessCertificateResource = Optional.of(accessCertificateService
                .createAccessCertificate(issuerNonceAuthentication, createAccessCertificateContext)
                .flatMap(resource -> accessCertificateService.signAccessCertificate(resource, issuerPrivateKey)))
                .map(Mono::block)
                .orElseThrow(() -> new AmvAccessRuntimeException("Could not create access certificate",
                        new IllegalStateException("Access certificate is not present")));

        log.info("Successfully signed access certificate {}", signedAccessCertificateResource.getUuid());

        return ResponseEntity.ok(true);
    }
}
