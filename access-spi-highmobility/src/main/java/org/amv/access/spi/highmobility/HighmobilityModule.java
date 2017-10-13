package org.amv.access.spi.highmobility;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.*;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.core.impl.DeviceCertificateImpl;
import org.amv.access.core.impl.IssuerImpl;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.CreateAccessCertificateRequest;
import org.amv.access.spi.CreateDeviceCertificateRequest;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer.CertificateIssuer;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.amv.access.util.MoreBase64.fromBase64OrThrow;
import static org.amv.access.util.MoreBase64.toBase64OrThrow;

@Slf4j
public class HighmobilityModule implements AmvAccessModuleSpi {

    private final CryptotoolWithIssuer cryptotool;

    private final Issuer issuer;

    public HighmobilityModule(CryptotoolWithIssuer cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);

        CertificateIssuer certificateIssuer = cryptotool.getCertificateIssuer();

        this.issuer = IssuerImpl.builder()
                .name(certificateIssuer.getName())
                .publicKeyBase64(toBase64OrThrow(certificateIssuer.getKeys().getPublicKey()))
                .build();
    }

    @Override
    public Mono<Boolean> isValidNonceAuth(NonceAuthentication auth, Device device) {
        requireNonNull(auth);
        requireNonNull(device);

        String devicePublicKey = fromBase64OrThrow(device.getPublicKeyBase64());

        return cryptotool.verifySignature(auth.getNonce(), auth.getSignedNonce(), devicePublicKey)
                .map(v -> v == Cryptotool.Validity.VALID);
    }

    @Override
    public Mono<DeviceCertificate> createDeviceCertificate(CreateDeviceCertificateRequest deviceCertificateRequest) {
        requireNonNull(deviceCertificateRequest);
        Application application = requireNonNull(deviceCertificateRequest.getApplication());
        Device device = requireNonNull(deviceCertificateRequest.getDevice());

        Cryptotool.DeviceCertificate deviceCertificate = cryptotool
                .createDeviceCertificate(application.getAppId(), device.getSerialNumber())
                .block();

        Cryptotool.Signature signedDeviceCertificate = cryptotool
                .generateSignature(deviceCertificate.getDeviceCertificate())
                .block();

        String deviceCertificateBase64 = hexToBase64(deviceCertificate.getDeviceCertificate())
                .orElseThrow(() -> new IllegalStateException("Could not convert device certificate to base64"));

        String signedDeviceCertificateBase64 = hexToBase64(signedDeviceCertificate.getSignature())
                .orElseThrow(() -> new IllegalStateException("Could not convert device certificate to base64"));

        DeviceCertificate deviceCertificateEntity = DeviceCertificateImpl.builder()
                .issuer(this.issuer)
                .application(application)
                .device(device)
                .certificateBase64(deviceCertificateBase64)
                .signedCertificateBase64(signedDeviceCertificateBase64)
                .build();

        return Mono.just(deviceCertificateEntity);

    }

    @Override
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest accessCertificateRequest) {
        requireNonNull(accessCertificateRequest);

        Application application = accessCertificateRequest.getApplication();
        Device device = requireNonNull(accessCertificateRequest.getDevice());
        Vehicle vehicle = requireNonNull(accessCertificateRequest.getVehicle());

        LocalDateTime validFrom = requireNonNull(accessCertificateRequest.getValidFrom());
        LocalDateTime validUntil = requireNonNull(accessCertificateRequest.getValidUntil());

        String vehiclePublicKey = fromBase64OrThrow(vehicle.getPublicKeyBase64());
        String devicePublicKey = fromBase64OrThrow(device.getPublicKeyBase64());

        Cryptotool.AccessCertificate deviceAccessCertificate = cryptotool.createAccessCertificate(
                vehicle.getSerialNumber(),
                vehiclePublicKey,
                device.getSerialNumber(),
                validFrom,
                validUntil).block();

        Cryptotool.AccessCertificate vehicleAccessCertificate = cryptotool.createAccessCertificate(
                device.getSerialNumber(),
                devicePublicKey,
                vehicle.getSerialNumber(),
                validFrom,
                validUntil).block();

        String signedDeviceAccessCertificate = Mono.just(deviceAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cryptotool::generateSignature)
                .map(Cryptotool.Signature::getSignature)
                .single()
                .block();

        String signedVehicleAccessCertificate = Mono.just(vehicleAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cryptotool::generateSignature)
                .map(Cryptotool.Signature::getSignature)
                .single()
                .block();

        AccessCertificate accessCertificateEntity = AccessCertificateImpl.builder()
                .issuer(this.issuer)
                .application(application)
                .vehicle(vehicle)
                .device(device)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .signedVehicleAccessCertificateBase64(toBase64OrThrow(signedVehicleAccessCertificate))
                .signedDeviceAccessCertificateBase64(toBase64OrThrow(signedDeviceAccessCertificate))
                .build();

        return Mono.just(accessCertificateEntity);
    }

    private Optional<String> hexToBase64(String value) {
        requireNonNull(value);

        try {
            return Optional.of(CryptotoolUtils.encodeHexAsBase64(value));
        } catch (Exception e) {
            log.warn("Could not decode hex string {}", value);
            return Optional.empty();
        }
    }
}
