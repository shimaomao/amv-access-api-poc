package org.amv.access.api.access;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.DeviceNonceAuthentication;
import org.amv.access.auth.IssuerNonceAuthentication;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.certificate.AccessCertificateService;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.SignedAccessCertificate;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.core.impl.SignedAccessCertificateImpl;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.exception.UnprocessableEntityException;
import org.amv.access.issuer.IssuerService;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.CreateAccessCertificateRequest;
import org.amv.access.spi.highmobility.AmvPermissionsAdapter;
import org.amv.access.spi.model.CreateAccessCertificateRequestImpl;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.PermissionsImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Slf4j
public class AccessCertificateServiceImpl implements AccessCertificateService {

    // TODO: move to a better place; do not hardcode
    private final static Cryptotool.Permissions amvStandardPermissions = PermissionsImpl.builder()
            .diagnosticsRead(true)
            .doorLocksRead(true)
            .doorLocksWrite(true)
            .keyfobPositionRead(true)
            .capabilitiesRead(true)
            .vehicleStatusRead(true)
            .chargeRead(true)
            .build();

    private final AmvAccessModuleSpi amvAccessModule;
    private final IssuerService issuerService;
    private final ApplicationRepository applicationRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;
    private final AccessCertificateRepository accessCertificateRepository;

    public AccessCertificateServiceImpl(
            AmvAccessModuleSpi amvAccessModule,
            IssuerService issuerService,
            ApplicationRepository applicationRepository,
            VehicleRepository vehicleRepository,
            DeviceRepository deviceRepository,
            DeviceCertificateRepository deviceCertificateRepository,
            AccessCertificateRepository accessCertificateRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.issuerService = requireNonNull(issuerService);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.vehicleRepository = requireNonNull(vehicleRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.deviceCertificateRepository = requireNonNull(deviceCertificateRepository);
        this.accessCertificateRepository = requireNonNull(accessCertificateRepository);
    }

    @Override
    @Transactional
    public Flux<SignedAccessCertificate> getAccessCertificates(DeviceNonceAuthentication nonceAuthentication) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");

        DeviceEntity device = deviceRepository.findBySerialNumber(nonceAuthentication.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device.getPublicKeyBase64());

        List<AccessCertificateEntity> accessCertificates = findValidAndRemoveExpired(device);

        if (!device.isEnabled()) {
            log.info("Removing {} access certificates for disabled device {}", accessCertificates.size(), device.getId());
            // remove access certificates for disabled devices instead of throwing error
            // so the device certificates will be removed from the device.
            accessCertificateRepository.delete(accessCertificates);
            return Flux.empty();
        }

        Map<Long, ApplicationEntity> applications = fetchApplications(accessCertificates);
        Map<Long, IssuerEntity> issuers = fetchIssuer(accessCertificates);
        Map<Long, VehicleEntity> vehicles = fetchVehicles(accessCertificates);

        return Flux.fromIterable(accessCertificates)
                .map(accessCertificateEntity -> {
                    IssuerEntity issuer = issuers.get(accessCertificateEntity.getIssuerId());
                    ApplicationEntity application = applications.get(accessCertificateEntity.getApplicationId());
                    VehicleEntity vehicle = vehicles.get(accessCertificateEntity.getVehicleId());

                    AccessCertificate accessCertificate = AccessCertificateImpl.builder()
                            .uuid(accessCertificateEntity.getUuid())
                            .name(vehicle.getName())
                            .deviceAccessCertificateBase64(accessCertificateEntity
                                    .getDeviceAccessCertificateBase64())
                            .vehicleAccessCertificateBase64(accessCertificateEntity
                                    .getVehicleAccessCertificateBase64())
                            .build();

                    SignedAccessCertificateImpl signedAccessCertificate = SignedAccessCertificateImpl.builder()
                            .accessCertificate(accessCertificate)
                            .signedDeviceAccessCertificateBase64(accessCertificateEntity
                                    .getSignedDeviceAccessCertificateBase64()
                                    .orElseThrow(IllegalStateException::new))
                            .signedVehicleAccessCertificateBase64(accessCertificateEntity
                                    .getSignedVehicleAccessCertificateBase64()
                                    .orElseThrow(IllegalStateException::new))
                            .build();

                    return signedAccessCertificate;
                });
    }

    @Override
    @Transactional
    public Mono<AccessCertificate> createAccessCertificate(IssuerNonceAuthentication nonceAuthentication,
                                                           CreateAccessCertificateContext context) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");
        requireNonNull(context, "`context` must not be null");

        IssuerEntity issuerEntity = findIssuerOrThrow(nonceAuthentication);

        verifyNonceAuthOrThrow(nonceAuthentication, issuerEntity.getPublicKeyBase64());

        VehicleEntity vehicleEntity = vehicleRepository.findOneBySerialNumber(context.getVehicleSerialNumber())
                .orElseThrow(() -> new NotFoundException("VehicleEntity not found"));

        verifyMatchingVehicleIssuerOrThrow(issuerEntity, vehicleEntity);

        ApplicationEntity applicationEntity = applicationRepository.findOneByAppId(context.getAppId())
                .orElseThrow(() -> new NotFoundException("ApplicationEntity not found"));

        DeviceEntity deviceEntity = deviceRepository.findBySerialNumber(context.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        DeviceCertificateEntity deviceCertificateEntity = deviceCertificateRepository
                .findOneByDeviceIdAndApplicationId(deviceEntity.getId(), applicationEntity.getId())
                .orElseThrow(() -> new NotFoundException("DeviceCertificateEntity not found"));

        boolean hasSameAppId = deviceCertificateEntity.getApplicationId() == applicationEntity.getId();
        if (!hasSameAppId) {
            throw new BadRequestException("Mismatching `appId`");
        }
        if (!applicationEntity.isEnabled()) {
            throw new UnprocessableEntityException("ApplicationEntity is disabled");
        }
        if (!deviceEntity.isEnabled()) {
            throw new UnprocessableEntityException("DeviceEntity is disabled");
        }

        CreateAccessCertificateRequest r = CreateAccessCertificateRequestImpl.builder()
                .device(deviceEntity)
                .vehicle(vehicleEntity)
                .validFrom(context.getValidityStart())
                .validUntil(context.getValidityEnd())
                .permissions(AmvPermissionsAdapter.builder()
                        .cryptotoolPermissions(amvStandardPermissions)
                        .build())
                .build();

        AccessCertificate accessCertificate = Optional.of(amvAccessModule.createAccessCertificate(r))
                .map(Mono::block)
                .orElseThrow(() -> new IllegalStateException("Could not create access certificate for " + context));

        AccessCertificateEntity accessCertificateEntity = AccessCertificateEntity.builder()
                .uuid(accessCertificate.getUuid())
                .issuerId(issuerEntity.getId())
                .applicationId(applicationEntity.getId())
                .vehicleId(vehicleEntity.getId())
                .deviceId(deviceEntity.getId())
                .validFrom(Date.from(r.getValidFrom().atZone(ZoneOffset.UTC).toInstant()))
                .validUntil(Date.from(r.getValidUntil().atZone(ZoneOffset.UTC).toInstant()))
                .deviceAccessCertificateBase64(accessCertificate.getDeviceAccessCertificateBase64())
                .vehicleAccessCertificateBase64(accessCertificate.getVehicleAccessCertificateBase64())
                .build();

        accessCertificateRepository.save(accessCertificateEntity);

        return Mono.just(accessCertificate);
    }

    @Override
    public Mono<Boolean> addAccessCertificateSignatures(IssuerNonceAuthentication nonceAuthentication,
                                                        AddAccessCertificateSignaturesContext context) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");
        requireNonNull(context, "`context` must not be null");

        IssuerEntity issuerEntity = findIssuerOrThrow(nonceAuthentication);
        verifyNonceAuthOrThrow(nonceAuthentication, issuerEntity.getPublicKeyBase64());

        AccessCertificateEntity accessCertificate = accessCertificateRepository
                .findByUuid(context.getAccessCertificateId().toString())
                .orElseThrow(() -> new NotFoundException("AccessCertificateEntity not found"));

        if (accessCertificate.getIssuerId() != issuerEntity.getId()) {
            log.warn("Mismatching issuer id for access cert {}: {} != {}", context.getAccessCertificateId(),
                    accessCertificate.getIssuerId(), issuerEntity.getId());
            // do not expose information about existing access certs - hence: NotFoundException
            throw new NotFoundException("AccessCertificateEntity not found");
        }

        verifySignatureOrThrow(issuerEntity,
                accessCertificate.getVehicleAccessCertificateBase64(),
                context.getVehicleAccessCertificateSignatureBase64(),
                "vehicle access certificate signature is invalid");

        verifySignatureOrThrow(issuerEntity,
                accessCertificate.getDeviceAccessCertificateBase64(),
                context.getDeviceAccessCertificateSignatureBase64(),
                "device access certificate signature is invalid");

        AccessCertificateEntity updatedAccessCertificateWithSignatures = accessCertificate.toBuilder()
                .vehicleAccessCertificateSignatureBase64(context.getVehicleAccessCertificateSignatureBase64())
                .deviceAccessCertificateSignatureBase64(context.getDeviceAccessCertificateSignatureBase64())
                .build();

        accessCertificateRepository.save(updatedAccessCertificateWithSignatures);

        if (log.isDebugEnabled()) {
            log.debug("saved access certificate: {}", updatedAccessCertificateWithSignatures);
        }

        return Mono.just(true);
    }

    @Override
    @Transactional
    public Mono<Boolean> revokeAccessCertificate(IssuerNonceAuthentication nonceAuthentication,
                                                 RevokeAccessCertificateContext context) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");
        requireNonNull(context, "`context` must not be null");

        IssuerEntity issuerEntity = findIssuerOrThrow(nonceAuthentication);
        verifyNonceAuthOrThrow(nonceAuthentication, issuerEntity.getPublicKeyBase64());

        AccessCertificateEntity accessCertificate = accessCertificateRepository
                .findByUuid(context.getAccessCertificateId().toString())
                .orElseThrow(() -> new NotFoundException("AccessCertificateEntity not found"));

        if (accessCertificate.getIssuerId() != issuerEntity.getId()) {
            log.warn("Mismatching issuer id for access cert {}: {} != {}", context.getAccessCertificateId(),
                    accessCertificate.getIssuerId(), issuerEntity.getId());
            // do not expose information about existing access certs - hence: NotFoundException
            throw new NotFoundException("AccessCertificateEntity not found");
        }

        accessCertificateRepository.delete(accessCertificate);

        return Mono.just(true);
    }

    @Override
    public Mono<String> createSignature(String messageBase64, String privateKeyBase64) {
        return amvAccessModule.createSignature(messageBase64, privateKeyBase64);
    }

    @Override
    public Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, String publicKeyBase64) {
        return amvAccessModule.verifySignature(messageBase64, signatureBase64, publicKeyBase64);
    }

    private IssuerEntity findIssuerOrThrow(IssuerNonceAuthentication nonceAuthentication) {
        return issuerService.findIssuerByUuid(UUID.fromString(nonceAuthentication.getIssuerUuid()))
                .orElseThrow(() -> new NotFoundException("IssuerEntity not found"));
    }

    private Map<Long, VehicleEntity> fetchVehicles(List<AccessCertificateEntity> accessCertificates) {
        return accessCertificates.stream()
                .map(AccessCertificateEntity::getVehicleId)
                .distinct()
                .map(id -> Optional.ofNullable(vehicleRepository.findOne(id)))
                .map(v -> v.orElseThrow(() -> new NotFoundException("VehicleEntity not found")))
                .collect(Collectors.toMap(VehicleEntity::getId, Function.identity()));
    }

    private Map<Long, IssuerEntity> fetchIssuer(List<AccessCertificateEntity> accessCertificates) {
        return accessCertificates.stream()
                .map(AccessCertificateEntity::getIssuerId)
                .distinct()
                .map(issuerService::findIssuerById)
                .map(v -> v.orElseThrow(() -> new NotFoundException("IssuerEntity not found")))
                .collect(Collectors.toMap(IssuerEntity::getId, Function.identity()));
    }

    private Map<Long, ApplicationEntity> fetchApplications(List<AccessCertificateEntity> accessCertificates) {
        return accessCertificates.stream()
                .map(AccessCertificateEntity::getApplicationId)
                .distinct()
                .map(id -> Optional.ofNullable(applicationRepository.findOne(id)))
                .map(v -> v.orElseThrow(() -> new NotFoundException("ApplicationEntity not found")))
                .collect(Collectors.toMap(ApplicationEntity::getId, Function.identity()));
    }

    private void verifySignatureOrThrow(IssuerEntity issuerEntity,
                                        String messageBase64,
                                        String signatureBase64,
                                        String errorMessage) {
        boolean isValidSignature = Optional.ofNullable(this.verifySignature(
                messageBase64,
                signatureBase64,
                issuerEntity.getPublicKeyBase64()))
                .map(Mono::block)
                .orElse(false);

        if (!isValidSignature) {
            throw new BadRequestException(errorMessage);
        }

        if (log.isDebugEnabled()) {
            log.debug("verified adding signature:\n" +
                            "issuer name: {}\n" +
                            "message: {}\n" +
                            "signature: {}",
                    issuerEntity.getName(),
                    messageBase64,
                    signatureBase64);
        }
    }

    private void verifyMatchingVehicleIssuerOrThrow(IssuerEntity issuerEntity, VehicleEntity vehicleEntity) {
        if (vehicleEntity.getIssuerId() != issuerEntity.getId()) {
            log.warn("Mismatching issuer id for vehicle {}: {} != {}",
                    vehicleEntity.getSerialNumber(),
                    vehicleEntity.getIssuerId(),
                    issuerEntity.getId());
            // do not expose information about existing vehicles - hence: NotFoundException
            throw new NotFoundException("VehicleEntity not found");
        }
    }

    private void verifyNonceAuthOrThrow(NonceAuthentication nonceAuthentication, String publicKeyBase64) {
        boolean isValidNonce = Optional.of(amvAccessModule.isValidNonceAuth(nonceAuthentication, publicKeyBase64))
                .map(Mono::block)
                .orElse(false);

        if (!isValidNonce) {
            throw new UnauthorizedException("Signature is invalid");
        }
    }

    private List<AccessCertificateEntity> findValidAndRemoveExpired(DeviceEntity device) {
        List<AccessCertificateEntity> accessCertificates = accessCertificateRepository
                .findByDeviceId(device.getId(), new PageRequest(0, Integer.MAX_VALUE))
                .getContent();

        List<AccessCertificateEntity> expiredAccessCertificates = accessCertificates.stream()
                .filter(AccessCertificateEntity::isExpired)
                .collect(Collectors.toList());

        accessCertificateRepository.delete(expiredAccessCertificates);

        List<AccessCertificateEntity> validAccessCertificates = accessCertificates.stream()
                .filter(a -> !a.isExpired())
                .filter(a -> a.getSignedDeviceAccessCertificateBase64().isPresent())
                .filter(a -> a.getSignedVehicleAccessCertificateBase64().isPresent())
                .collect(Collectors.toList());

        return ImmutableList.copyOf(validAccessCertificates);
    }
}
