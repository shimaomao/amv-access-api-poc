package org.amv.access.api.access;

import org.amv.access.model.*;
import org.amv.access.api.access.model.CreateAccessCertificateRequest;
import org.amv.access.api.access.model.GetAccessCertificateRequest;
import org.amv.access.api.access.model.RevokeAccessCertificateRequest;
import org.amv.access.spi.AmvAccessModuleSpi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

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
        this.vehicleRepository = vehicleRepository;
        this.deviceRepository = requireNonNull(deviceRepository);
        this.accessCertificateRepository = requireNonNull(accessCertificateRepository);
        this.accessCertificateRequestRepository = requireNonNull(accessCertificateRequestRepository);
    }

    @Override
    public Flux<AccessCertificate> getAccessCertificates(GetAccessCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        return Mono.fromCallable(() -> accessCertificateRepository
                .findByDeviceSerialNumberAndVehicleSerialNumber(
                        request.getAccessGainingSerialNumber(),
                        request.getAccessProvidingSerialNumber()))
                .flatMap(Flux::fromIterable);
    }

    @Override
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        Mono<Vehicle> vehicleMono = Mono.just(request.getVehicleSerialNumber())
                .map(vehicleRepository::findBySerialNumber)
                .map(vehicleOptional -> vehicleOptional
                        .orElseThrow(() -> new ValidationException("Vehicle not found"))
                );

        Mono<Device> deviceMono = Mono.just(request.getDeviceSerialNumber())
                .map(deviceRepository::findBySerialNumber)
                .map(vehicleOptional -> vehicleOptional
                        .orElseThrow(() -> new ValidationException("Device not found"))
                ).doOnNext(device -> {
                    boolean hasSameAppId = Objects.equals(device.getAppId(), request.getAppId());
                    if (!hasSameAppId) {
                        throw new ValidationException("Mismatching `appId`");
                    }
                });

        return Mono.when(deviceMono, vehicleMono)
                .map(deviceAndVehicle -> Tuples.of(
                        saveAccessCertificateRequest(request).block(),
                        deviceAndVehicle.getT1(),
                        deviceAndVehicle.getT2()))
                .flatMap(deviceAndVehicleAndCertificate ->
                        amvAccessModule.createAccessCertificate(
                                deviceAndVehicleAndCertificate.getT1(),
                                deviceAndVehicleAndCertificate.getT2(),
                                deviceAndVehicleAndCertificate.getT3()
                        ))
                .single()
                .map(accessCertificateRepository::save);
    }

    @Override
    public Mono<Void> revokeAccessCertificate(RevokeAccessCertificateRequest request) {
        throw new UnsupportedOperationException();
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

        return Mono.fromCallable(() -> accessCertificateRequestRepository.save(accessCertificateRequestEntity));
    }

    private Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
