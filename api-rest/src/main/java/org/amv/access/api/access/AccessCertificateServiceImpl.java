package org.amv.access.api.access;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.certificate.AccessCertificateService;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.impl.AccessCertificateImpl;
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
    public Flux<AccessCertificate> getAccessCertificates(NonceAuthentication nonceAuthentication,
                                                         GetAccessCertificateContext request) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");
        requireNonNull(request, "`request` must not be null");

        DeviceEntity device = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device);

        if (!device.isEnabled()) {
            throw new UnprocessableEntityException("DeviceEntity is disabled");
        }

        List<AccessCertificateEntity> accessCertificates = accessCertificateRepository
                .findByDeviceId(device.getId(), new PageRequest(0, Integer.MAX_VALUE))
                .getContent();

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
                            .signedDeviceAccessCertificateBase64(accessCertificate.getSignedDeviceAccessCertificateBase64())
                            .signedVehicleAccessCertificateBase64(accessCertificate.getSignedVehicleAccessCertificateBase64())
                            .build();
                });
    }

    @Override
    @Transactional
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateContext context) {
        requireNonNull(context, "`context` must not be null");

        ApplicationEntity applicationEntity = applicationRepository.findOneByAppId(context.getAppId())
                .orElseThrow(() -> new NotFoundException("ApplicationEntity not found"));

        VehicleEntity vehicleEntity = vehicleRepository.findOneBySerialNumber(context.getVehicleSerialNumber())
                .orElseThrow(() -> new NotFoundException("VehicleEntity not found"));

        IssuerEntity issuerEntity = issuerService.findIssuerById(vehicleEntity.getIssuerId())
                .orElseThrow(() -> new NotFoundException("IssuerEntity not found"));

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
                .issuer(issuerEntity)
                .application(applicationEntity)
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
                .validFrom(r.getValidFrom())
                .validUntil(r.getValidUntil())
                .signedDeviceAccessCertificateBase64(accessCertificate.getSignedDeviceAccessCertificateBase64())
                .signedVehicleAccessCertificateBase64(accessCertificate.getSignedVehicleAccessCertificateBase64())
                .build();

        accessCertificateRepository.save(accessCertificateEntity);

        return Mono.just(accessCertificate);
    }

    @Override
    @Transactional
    public Mono<Void> revokeAccessCertificate(NonceAuthentication nonceAuthentication,
                                              RevokeAccessCertificateContext context) {
        requireNonNull(nonceAuthentication, "`nonceAuthentication` must not be null");
        requireNonNull(context, "`context` must not be null");

        DeviceEntity device = deviceRepository.findBySerialNumber(context.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device);

        if (!device.isEnabled()) {
            log.warn("Allowing disabled device {} to revoke access certificate {}", device.getId(),
                    context.getAccessCertificateId());
        }

        AccessCertificateEntity accessCertificate = accessCertificateRepository
                .findByUuid(context.getAccessCertificateId().toString())
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
}
