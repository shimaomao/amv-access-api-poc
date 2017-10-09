package org.amv.access.api.access;

import org.amv.access.api.access.model.CreateAccessCertificateRequest;
import org.amv.access.api.access.model.GetAccessCertificateRequest;
import org.amv.access.api.access.model.RevokeAccessCertificateRequest;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.exception.UnauthorizedException;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class AccessCertificateServiceImpl implements AccessCertificateService {

    private final AmvAccessModuleSpi amvAccessModule;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;
    private final AccessCertificateRepository accessCertificateRepository;
    private final AccessCertificateRequestRepository accessCertificateRequestRepository;

    public AccessCertificateServiceImpl(
            AmvAccessModuleSpi amvAccessModule,
            VehicleRepository vehicleRepository,
            DeviceRepository deviceRepository,
            AccessCertificateRepository accessCertificateRepository,
            AccessCertificateRequestRepository accessCertificateRequestRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
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

        Device device = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("Device not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device);

        List<AccessCertificate> accessCertificates = accessCertificateRepository
                .findByDeviceSerialNumber(device.getSerialNumber());

        return Flux.fromIterable(accessCertificates);
    }

    @Override
    @Transactional
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        Mono<Vehicle> vehicleMono = Mono.just(request.getVehicleSerialNumber())
                .map(vehicleRepository::findBySerialNumber)
                .map(vehicleOptional -> vehicleOptional
                        .orElseThrow(() -> new NotFoundException("Vehicle not found"))
                );

        Mono<Device> deviceMono = Mono.just(request.getDeviceSerialNumber())
                .map(deviceRepository::findBySerialNumber)
                .map(vehicleOptional -> vehicleOptional
                        .orElseThrow(() -> new NotFoundException("Device not found"))
                ).doOnNext(device -> {
                    boolean hasSameAppId = Objects.equals(device.getAppId(), request.getAppId());
                    if (!hasSameAppId) {
                        throw new BadRequestException("Mismatching `appId`");
                    }
                });

        AccessCertificate accessCertificate = Mono.when(deviceMono, vehicleMono)
                .map(deviceAndVehicle -> Tuples.of(
                        saveAccessCertificateRequest(request).block(),
                        deviceAndVehicle.getT1(),
                        deviceAndVehicle.getT2()))
                .flatMapMany(deviceAndVehicleAndCertificate ->
                        amvAccessModule.createAccessCertificate(
                                deviceAndVehicleAndCertificate.getT1(),
                                deviceAndVehicleAndCertificate.getT2(),
                                deviceAndVehicleAndCertificate.getT3()
                        ))
                .single()
                .block();

        AccessCertificate savedEntity = accessCertificateRepository.save(accessCertificate);

        return Mono.just(savedEntity);
    }

    @Override
    @Transactional
    public Mono<Void> revokeAccessCertificate(NonceAuthentication nonceAuthentication,
                                              RevokeAccessCertificateRequest request) {
        requireNonNull(request);

        Device device = deviceRepository.findBySerialNumber(request.getDeviceSerialNumber())
                .orElseThrow(() -> new NotFoundException("Device not found"));

        verifyNonceAuthOrThrow(nonceAuthentication, device);

        AccessCertificate accessCertificate = accessCertificateRepository
                .findByUuid(request.getAccessCertificateId().toString())
                .orElseThrow(() -> new NotFoundException("Access Certificate not found"));

        if (!accessCertificate.getDeviceSerialNumber().equals(device.getSerialNumber())) {
            // do not expose information about existing access certs - hence: NotFoundException
            throw new NotFoundException("Access Certificate not found");
        }

        accessCertificateRepository.delete(accessCertificate);

        return Mono.empty();
    }

    private void verifyNonceAuthOrThrow(NonceAuthentication nonceAuthentication, Device device) {
        boolean isValidNonce = Optional.of(amvAccessModule.isValidNonceAuth(nonceAuthentication, device))
                .map(Mono::block)
                .orElse(false);
        
        if (!isValidNonce) {
            throw new UnauthorizedException("Signature is invalid");
        }
    }

    private Mono<AccessCertificateRequest> saveAccessCertificateRequest(CreateAccessCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        AccessCertificateRequest accessCertificateRequestEntity = AccessCertificateRequest.builder()
                .appId(request.getAppId())
                .deviceSerialNumber(request.getDeviceSerialNumber())
                .vehicleSerialNumber(request.getVehicleSerialNumber())
                .validFrom(localDateTimeToDate(request.getValidityStart()))
                .validUntil(localDateTimeToDate(request.getValidityEnd()))
                .build();

        AccessCertificateRequest savedEntity = accessCertificateRequestRepository.save(accessCertificateRequestEntity);

        return Mono.just(savedEntity);
    }

    private Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
