package org.amv.access.api.access;

import org.amv.access.api.access.model.CreateAccessCertificateRequest;
import org.amv.access.api.access.model.GetAccessCertificateRequest;
import org.amv.access.api.access.model.RevokeAccessCertificateRequest;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.AccessCertificate;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.model.CreateAccessCertificateRequestImpl;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class AccessCertificateServiceImpl implements AccessCertificateService {

    private final AmvAccessModuleSpi amvAccessModule;
    private final IssuerRepository issuerRepository;
    private final ApplicationRepository applicationRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;
    private final AccessCertificateRepository accessCertificateRepository;
    private final AccessCertificateRequestRepository accessCertificateRequestRepository;

    public AccessCertificateServiceImpl(
            AmvAccessModuleSpi amvAccessModule,
            IssuerRepository issuerRepository,
            ApplicationRepository applicationRepository,
            VehicleRepository vehicleRepository,
            DeviceRepository deviceRepository,
            AccessCertificateRepository accessCertificateRepository,
            AccessCertificateRequestRepository accessCertificateRequestRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.issuerRepository = requireNonNull(issuerRepository);
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
                .map(v -> v.orElseThrow(() -> new NotFoundException("Application not found")))
                .collect(Collectors.toMap(ApplicationEntity::getId, Function.identity()));

        Map<Long, VehicleEntity> vehicles = accessCertificates.stream()
                .map(AccessCertificateEntity::getVehicleId)
                .distinct()
                .map(id -> Optional.ofNullable(vehicleRepository.findOne(id)))
                .map(v -> v.orElseThrow(() -> new NotFoundException("Vehicle not found")))
                .collect(Collectors.toMap(VehicleEntity::getId, Function.identity()));

        accessCertificates.stream()
                .map(accessCertificate -> {
                    ApplicationEntity application = applications.get(accessCertificate.getApplicationId());
                    VehicleEntity vehicle = vehicles.get(accessCertificate.getId());

                    return AccessCertificateImpl.builder()
                            .application(application)
                            .device(device)
                            .vehicle(vehicle)
                            .signedDeviceAccessCertificateBase64(accessCertificate.getSignedVehicleAccessCertificateBase64())
                            .signedVehicleAccessCertificateBase64(accessCertificate.getSignedDeviceAccessCertificateBase64())
                            .build();
                });

        return Flux.fromIterable(accessCertificates)
                .map(a -> AccessCertificateImpl.builder()
                        .build());
    }

    @Override
    @Transactional
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        ApplicationEntity application = applicationRepository.findOneByAppId(request.getAppId())
                .orElseThrow(() -> new NotFoundException("ApplicationEntity with given appId not found"));

        VehicleEntity vehicle = vehicleRepository.findOneBySerialNumber(request.getVehicleSerialNumber())
                .orElseThrow(() -> new NotFoundException("VehicleEntity not found"));

        DeviceEntity device = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("Device not found"));

        boolean hasSameAppId = Objects.equals(device.getAppId(), request.getAppId());
        if (!hasSameAppId) {
            throw new BadRequestException("Mismatching `appId`");
        }

        saveAccessCertificateRequest(request).block();

        CreateAccessCertificateRequestImpl r = CreateAccessCertificateRequestImpl.builder()
                .application(application)
                .device(device)
                .vehicle(vehicle)
                .validFrom(request.getValidityStart())
                .validUntil(request.getValidityEnd())
                .build();

        AccessCertificate accessCertificate = Optional.of(amvAccessModule.createAccessCertificate(r))
                .map(Mono::block)
                .orElseThrow(() -> new IllegalStateException("Could not create access certifcate for " + request));

        AccessCertificateEntity accessCertificateEntity = AccessCertificateEntity.builder()
                .applicationId(application.getId())
                .vehicleId(vehicle.getId())
                .deviceId(device.getId())
                .signedVehicleAccessCertificateBase64(accessCertificate.getSignedDeviceAccessCertificateBase64())
                .signedDeviceAccessCertificateBase64(accessCertificate.getSignedDeviceAccessCertificateBase64())
                .build();

        accessCertificateRepository.save(accessCertificateEntity);

        return Mono.just(accessCertificate);
    }

    @Override
    @Transactional
    public Mono<Void> revokeAccessCertificate(NonceAuthentication nonceAuthentication,
                                              RevokeAccessCertificateRequest request) {
        requireNonNull(request);

        DeviceEntity device = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("DeviceEntity not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device);

        AccessCertificateEntity accessCertificate = accessCertificateRepository
                .findByUuid(request.getAccessCertificateId().toString())
                .orElseThrow(() -> new NotFoundException("Access Certificate not found"));

        if (accessCertificate.getDeviceId() != device.getId()) {
            // do not expose information about existing access certs - hence: NotFoundException
            throw new NotFoundException("Access Certificate not found");
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

        AccessCertificateRequestEntity accessCertificateRequestEntityEntity = AccessCertificateRequestEntity.builder()
                .appId(request.getAppId())
                .deviceSerialNumber(request.getDeviceSerialNumber())
                .vehicleSerialNumber(request.getVehicleSerialNumber())
                .validFrom(localDateTimeToDate(request.getValidityStart()))
                .validUntil(localDateTimeToDate(request.getValidityEnd()))
                .build();

        AccessCertificateRequestEntity savedEntity = accessCertificateRequestRepository.save(accessCertificateRequestEntityEntity);

        return Mono.just(savedEntity);
    }

    private Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
