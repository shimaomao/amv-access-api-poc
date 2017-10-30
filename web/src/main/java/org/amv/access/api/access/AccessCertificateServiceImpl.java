package org.amv.access.api.access;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.access.model.CreateAccessCertificateRequestDto;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.issuer.IssuerService;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.highmobility.AmvPermissionsAdapter;
import org.amv.access.spi.model.CreateAccessCertificateRequestImpl;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.PermissionsImpl;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final AccessCertificateRepository accessCertificateRepository;
    private final AccessCertificateRequestRepository accessCertificateRequestRepository;

    public AccessCertificateServiceImpl(
            AmvAccessModuleSpi amvAccessModule,
            IssuerService issuerService,
            ApplicationRepository applicationRepository,
            VehicleRepository vehicleRepository,
            DeviceRepository deviceRepository,
            AccessCertificateRepository accessCertificateRepository,
            AccessCertificateRequestRepository accessCertificateRequestRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.issuerService = requireNonNull(issuerService);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.vehicleRepository = requireNonNull(vehicleRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.accessCertificateRepository = requireNonNull(accessCertificateRepository);
        this.accessCertificateRequestRepository = requireNonNull(accessCertificateRequestRepository);
    }

    @Override
    @Transactional
    public Flux<AccessCertificate> getAccessCertificates(NonceAuthentication nonceAuthentication,
                                                         GetAccessCertificateRequest request) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");
        requireNonNull(request, "`request` must not be null");

        DeviceEntity device = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device);

        List<AccessCertificateEntity> accessCertificates = accessCertificateRepository
                .findByDeviceId(device.getId());

        Map<Long, ApplicationEntity> applications = accessCertificates.stream()
                .map(AccessCertificateEntity::getApplicationId)
                .distinct()
                .map(id -> Optional.ofNullable(applicationRepository.findOne(id)))
                .map(v -> v.orElseThrow(() -> new NotFoundException("ApplicationEntity not found")))
                .collect(Collectors.toMap(ApplicationEntity::getId, Function.identity()));

        Map<Long, IssuerEntity> issuers = accessCertificates.stream()
                .map(AccessCertificateEntity::getIssuerId)
                .distinct()
                .map(issuerService::findIssuerById)
                .map(v -> v.orElseThrow(() -> new NotFoundException("IssuerEntity not found")))
                .collect(Collectors.toMap(IssuerEntity::getId, Function.identity()));

        Map<Long, VehicleEntity> vehicles = accessCertificates.stream()
                .map(AccessCertificateEntity::getVehicleId)
                .distinct()
                .map(id -> Optional.ofNullable(vehicleRepository.findOne(id)))
                .map(v -> v.orElseThrow(() -> new NotFoundException("VehicleEntity not found")))
                .collect(Collectors.toMap(VehicleEntity::getId, Function.identity()));

        return Flux.fromIterable(accessCertificates)
                .map(accessCertificate -> {
                    IssuerEntity issuer = issuers.get(accessCertificate.getIssuerId());
                    ApplicationEntity application = applications.get(accessCertificate.getApplicationId());
                    VehicleEntity vehicle = vehicles.get(accessCertificate.getVehicleId());

                    return AccessCertificateImpl.builder()
                            .uuid(accessCertificate.getUuid())
                            .issuer(issuer)
                            .application(application)
                            .device(device)
                            .vehicle(vehicle)
                            .validFrom(accessCertificate.getValidFrom())
                            .validUntil(accessCertificate.getValidUntil())
                            .deviceAccessCertificateBase64(accessCertificate.getDeviceAccessCertificateBase64())
                            .deviceAccessCertificateSignatureBase64(accessCertificate.getDeviceAccessCertificateSignatureBase64())
                            .vehicleAccessCertificateBase64(accessCertificate.getVehicleAccessCertificateBase64())
                            .vehicleAccessCertificateSignatureBase64(accessCertificate.getVehicleAccessCertificateSignatureBase64())
                            .signedDeviceAccessCertificateBase64(accessCertificate.getSignedDeviceAccessCertificateBase64())
                            .signedVehicleAccessCertificateBase64(accessCertificate.getSignedVehicleAccessCertificateBase64())
                            .build();
                });
    }

    @Override
    @Transactional
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        ApplicationEntity applicationEntity = applicationRepository.findOneByAppId(request.getAppId())
                .orElseThrow(() -> new NotFoundException("ApplicationEntity not found"));

        VehicleEntity vehicleEntity = vehicleRepository.findOneBySerialNumber(request.getVehicleSerialNumber())
                .orElseThrow(() -> new NotFoundException("VehicleEntity not found"));

        DeviceEntity deviceEntity = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        IssuerEntity issuerEntity = issuerService.findIssuerById(vehicleEntity.getIssuerId())
                .orElseThrow(() -> new NotFoundException("IssuerEntity not found"));

        boolean hasSameAppId = deviceEntity.getApplicationId() == applicationEntity.getId();
        if (!hasSameAppId) {
            throw new BadRequestException("Mismatching `appId`");
        }

        saveAccessCertificateRequest(request).block();

        CreateAccessCertificateRequestImpl r = CreateAccessCertificateRequestImpl.builder()
                .issuer(issuerEntity)
                .application(applicationEntity)
                .device(deviceEntity)
                .vehicle(vehicleEntity)
                .validFrom(request.getValidityStart())
                .validUntil(request.getValidityEnd())
                .permissions(AmvPermissionsAdapter.builder()
                        .cryptotoolPermissions(amvStandardPermissions)
                        .build())
                .build();

        AccessCertificate accessCertificate = Optional.of(amvAccessModule.createAccessCertificate(r))
                .map(Mono::block)
                .orElseThrow(() -> new IllegalStateException("Could not create access certificate for " + request));

        AccessCertificateEntity accessCertificateEntity = AccessCertificateEntity.builder()
                .uuid(accessCertificate.getUuid())
                .issuerId(issuerEntity.getId())
                .applicationId(applicationEntity.getId())
                .vehicleId(vehicleEntity.getId())
                .deviceId(deviceEntity.getId())
                .validFrom(r.getValidFrom())
                .validUntil(r.getValidUntil())
                .deviceAccessCertificateBase64(accessCertificate.getDeviceAccessCertificateBase64())
                .deviceAccessCertificateSignatureBase64(accessCertificate.getDeviceAccessCertificateSignatureBase64())
                .vehicleAccessCertificateBase64(accessCertificate.getVehicleAccessCertificateBase64())
                .vehicleAccessCertificateSignatureBase64(accessCertificate.getVehicleAccessCertificateSignatureBase64())
                .signedDeviceAccessCertificateBase64(accessCertificate.getSignedDeviceAccessCertificateBase64())
                .signedVehicleAccessCertificateBase64(accessCertificate.getSignedVehicleAccessCertificateBase64())
                .build();

        accessCertificateRepository.save(accessCertificateEntity);

        return Mono.just(accessCertificate);
    }

    @Override
    @Transactional
    public Mono<Void> revokeAccessCertificate(NonceAuthentication nonceAuthentication,
                                              RevokeAccessCertificateRequest request) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");
        requireNonNull(request, "`request` must not be null");

        DeviceEntity device = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device);

        AccessCertificateEntity accessCertificate = accessCertificateRepository
                .findByUuid(request.getAccessCertificateId().toString())
                .orElseThrow(() -> new NotFoundException("AccessCertificateEntity not found"));

        if (accessCertificate.getDeviceId() != device.getId()) {
            // do not expose information about existing access certs - hence: NotFoundException
            throw new NotFoundException("AccessCertificateEntity not found");
        }

        accessCertificateRepository.delete(accessCertificate);

        return Mono.empty();
    }

    private void verifyNonceAuthOrThrow(NonceAuthentication nonceAuthentication, DeviceEntity device) {
        boolean isValidNonce = Optional.of(amvAccessModule.isValidNonceAuth(nonceAuthentication, device))
                .map(Mono::block)
                .orElse(false);

        if (!isValidNonce) {
            throw new UnauthorizedException("Signature is invalid");
        }
    }

    private Mono<AccessCertificateRequestEntity> saveAccessCertificateRequest(CreateAccessCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        AccessCertificateRequestEntity accessCertificateRequestEntity = AccessCertificateRequestEntity.builder()
                .appId(request.getAppId())
                .deviceSerialNumber(request.getDeviceSerialNumber())
                .vehicleSerialNumber(request.getVehicleSerialNumber())
                .validFrom(request.getValidityStart())
                .validUntil(request.getValidityEnd())
                .build();

        AccessCertificateRequestEntity savedEntity = accessCertificateRequestRepository.save(accessCertificateRequestEntity);

        return Mono.just(savedEntity);
    }
}
