package org.amv.access.spi.highmobility;

import com.google.common.base.Charsets;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

import static java.util.Objects.requireNonNull;

public class HighmobilityModule implements AmvAccessModuleSpi {

    private final CryptotoolWithIssuer cryptotool;

    public HighmobilityModule(CryptotoolWithIssuer cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);
    }

    @Override
    public Mono<DeviceCertificate> createDeviceCertificate(Application application, Device deviceEntity) {
        Cryptotool.DeviceCertificate deviceCertificate = cryptotool.createDeviceCertificate(
                application.getAppId(),
                deviceEntity.getSerialNumber()
        ).block();

        Cryptotool.Signature signedDeviceCertificate = cryptotool
                .generateSignature(deviceCertificate.getDeviceCertificate())
                .block();

        String deviceCertificateBase64 = Base64.getEncoder()
                .encodeToString(deviceCertificate.getDeviceCertificate()
                        .getBytes(Charsets.UTF_8));

        String signedDeviceCertificateBase64 = Base64.getEncoder()
                .encodeToString(signedDeviceCertificate.getSignature()
                        .getBytes(Charsets.UTF_8));

        CryptotoolWithIssuer.CertificateIssuer certificateIssuer = cryptotool.getCertificateIssuer();

        DeviceCertificate deviceCertificateEntity = DeviceCertificate.builder()
                .certificate(deviceCertificate.getDeviceCertificate())
                .certificateBase64(deviceCertificateBase64)
                .signedCertificateBase64(signedDeviceCertificateBase64)
                .appId(deviceEntity.getAppId())
                .issuerName(certificateIssuer.getName())
                .issuerPublicKeyBase64(certificateIssuer.getPublicKeyBase64())
                .deviceName(deviceEntity.getName())
                .deviceSerialNumber(deviceEntity.getSerialNumber())
                .build();

        return Mono.just(deviceCertificateEntity);

    }

    @Override
    public Mono<AccessCertificate> createAccessCertificate(AccessCertificateRequest accessCertificateRequest, Device device, Vehicle vehicle) {
        LocalDateTime validFrom = LocalDateTime.ofInstant(accessCertificateRequest
                        .getValidFrom()
                        .toInstant(),
                ZoneId.systemDefault());

        LocalDateTime validUntil = LocalDateTime.ofInstant(accessCertificateRequest
                        .getValidUntil()
                        .toInstant(),
                ZoneId.systemDefault());

        Cryptotool.AccessCertificate deviceAccessCertificate = cryptotool.createAccessCertificate(
                vehicle.getSerialNumber(),
                vehicle.getPublicKey(),
                device.getSerialNumber(),
                validFrom,
                validUntil)
                .block();

        String signedDeviceAccessCertificateBase64 = Mono.just(deviceAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cryptotool::generateSignature)
                .map(Cryptotool.Signature::getSignature)
                .map(signedVehicleCertificate -> Base64.getEncoder()
                        .encodeToString(signedVehicleCertificate
                                .getBytes(Charsets.UTF_8)))
                .single()
                .block();

        Cryptotool.AccessCertificate vehicleAccessCertificate = cryptotool.createAccessCertificate(
                device.getSerialNumber(),
                device.getPublicKey(),
                vehicle.getSerialNumber(),
                validFrom,
                validUntil
        ).block();

        String signedVehicleAccessCertificateBase64 = Mono.just(vehicleAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cryptotool::generateSignature)
                .map(Cryptotool.Signature::getSignature)
                .map(signedVehicleCertificate -> Base64.getEncoder()
                        .encodeToString(signedVehicleCertificate
                                .getBytes(Charsets.UTF_8)))
                .single()
                .block();

        AccessCertificate accessCertificateEntity = AccessCertificate.builder()
                .appId(accessCertificateRequest.getAppId())
                .vehicleSerialNumber(vehicle.getSerialNumber())
                .deviceSerialNumber(device.getSerialNumber())
                .validFrom(validFrom)
                .validUntil(validUntil)
                .vehicleCertificate(vehicleAccessCertificate.getAccessCertificate())
                .signedVehicleCertificateBase64(signedVehicleAccessCertificateBase64)
                .deviceCertificate(deviceAccessCertificate.getAccessCertificate())
                .signedDeviceCertificateBase64(signedDeviceAccessCertificateBase64)
                .build();

        return Mono.just(accessCertificateEntity);
    }
}
