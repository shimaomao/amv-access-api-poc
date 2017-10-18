package org.amv.access.spi.highmobility;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.*;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.core.impl.DeviceCertificateImpl;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.CreateAccessCertificateRequest;
import org.amv.access.spi.CreateDeviceCertificateRequest;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.decodeBase64AsHex;

@Slf4j
public class HighmobilityModule implements AmvAccessModuleSpi {

    private final Cryptotool cryptotool;

    public HighmobilityModule(Cryptotool cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);
    }

    @Override
    public Mono<Boolean> isValidNonceAuth(NonceAuthentication auth, Device device) {
        requireNonNull(auth);
        requireNonNull(device);

        String devicePublicKey = decodeBase64AsHex(device.getPublicKeyBase64());
        String nonce = decodeBase64AsHex(auth.getNonceBase64());
        String signature = decodeBase64AsHex(auth.getNonceSignatureBase64());

        return cryptotool.verifySignature(nonce, signature, devicePublicKey)
                .map(v -> v == Cryptotool.Validity.VALID);
    }

    @Override
    public Mono<DeviceCertificate> createDeviceCertificate(CreateDeviceCertificateRequest deviceCertificateRequest) {
        requireNonNull(deviceCertificateRequest);

        Issuer issuer = requireNonNull(deviceCertificateRequest.getIssuer());
        Application application = requireNonNull(deviceCertificateRequest.getApplication());
        Device device = requireNonNull(deviceCertificateRequest.getDevice());

        Cryptotool.DeviceCertificate deviceCertificate = cryptotool
                .createDeviceCertificate(issuer.getNameInHex(),
                        application.getAppId(),
                        device.getSerialNumber(),
                        decodeBase64AsHex(device.getPublicKeyBase64()))
                .block();

        Cryptotool.Signature signature = cryptotool
                .generateSignature(deviceCertificate.getDeviceCertificate(), decodeBase64AsHex(issuer.getPrivateKeyBase64()))
                .block();

        String fullDeviceCertificate = deviceCertificate.getDeviceCertificate() + signature.getSignature();
        String fullDeviceCertificateBase64 = hexToBase64(fullDeviceCertificate)
                .orElseThrow(() -> new IllegalStateException("Could not convert full device certificate to base64"));

        String deviceCertificateBase64 = hexToBase64(deviceCertificate.getDeviceCertificate())
                .orElseThrow(() -> new IllegalStateException("Could not convert device certificate to base64"));

        String deviceCertificateSignatureBase64 = hexToBase64(signature.getSignature())
                .orElseThrow(() -> new IllegalStateException("Could not convert device certificate signature to base64"));

        DeviceCertificate deviceCertificateEntity = DeviceCertificateImpl.builder()
                .issuer(issuer)
                .application(application)
                .device(device)
                .certificateBase64(deviceCertificateBase64)
                .certificateSignatureBase64(deviceCertificateSignatureBase64)
                .fullDeviceCertificateBase64(fullDeviceCertificateBase64)
                .build();

        return Mono.just(deviceCertificateEntity);
    }

    @Override
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest accessCertificateRequest) {
        requireNonNull(accessCertificateRequest);

        Issuer issuer = requireNonNull(accessCertificateRequest.getIssuer());
        Application application = requireNonNull(accessCertificateRequest.getApplication());
        Device device = requireNonNull(accessCertificateRequest.getDevice());
        Vehicle vehicle = requireNonNull(accessCertificateRequest.getVehicle());

        LocalDateTime validFrom = requireNonNull(accessCertificateRequest.getValidFrom());
        LocalDateTime validUntil = requireNonNull(accessCertificateRequest.getValidUntil());

        String vehiclePublicKey = CryptotoolUtils.decodeBase64AsHex(vehicle.getPublicKeyBase64());
        String devicePublicKey = CryptotoolUtils.decodeBase64AsHex(device.getPublicKeyBase64());

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

        String issuerPrivateKeyInHey = decodeBase64AsHex(issuer.getPrivateKeyBase64());
        String deviceAccessCertificateSignature = Mono.just(deviceAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cert -> cryptotool.generateSignature(cert, issuerPrivateKeyInHey))
                .map(Cryptotool.Signature::getSignature)
                .single()
                .block();

        String vehicleAccessCertificateSignature = Mono.just(vehicleAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cert -> cryptotool.generateSignature(cert, issuerPrivateKeyInHey))
                .map(Cryptotool.Signature::getSignature)
                .single()
                .block();

        String fullDeviceAccessCertificate = deviceAccessCertificate.getAccessCertificate() + deviceAccessCertificateSignature;
        String fullDeviceAccessCertificateBase64 = hexToBase64(fullDeviceAccessCertificate)
                .orElseThrow(() -> new IllegalStateException("Could not convert full device access certificate to base64"));

        String fullVehicleAccessCertificate = vehicleAccessCertificate.getAccessCertificate() + vehicleAccessCertificateSignature;
        String fullVehicleAccessCertificateBase64 = hexToBase64(fullVehicleAccessCertificate)
                .orElseThrow(() -> new IllegalStateException("Could not convert full vehicle access certificate to base64"));

        String deviceAccessCertificateBase64 = hexToBase64(deviceAccessCertificate.getAccessCertificate())
                .orElseThrow(() -> new IllegalStateException("Could not convert device access certificate to base64"));

        String deviceAccessCertificateSignatureBase64 = hexToBase64(deviceAccessCertificateSignature)
                .orElseThrow(() -> new IllegalStateException("Could not convert device access certificate signature to base64"));

        String vehicleAccessCertificateBase64 = hexToBase64(vehicleAccessCertificate.getAccessCertificate())
                .orElseThrow(() -> new IllegalStateException("Could not convert vehicle access certificate to base64"));

        String vehicleAccessCertificateSignatureBase64 = hexToBase64(vehicleAccessCertificateSignature)
                .orElseThrow(() -> new IllegalStateException("Could not convert vehicle access certificate signature to base64"));

        AccessCertificate accessCertificateEntity = AccessCertificateImpl.builder()
                .uuid(UUID.randomUUID().toString())
                .issuer(issuer)
                .application(application)
                .vehicle(vehicle)
                .device(device)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .deviceAccessCertificateBase64(deviceAccessCertificateBase64)
                .deviceAccessCertificateSignatureBase64(deviceAccessCertificateSignatureBase64)
                .vehicleAccessCertificateBase64(vehicleAccessCertificateBase64)
                .vehicleAccessCertificateSignatureBase64(vehicleAccessCertificateSignatureBase64)
                .fullDeviceAccessCertificateBase64(fullDeviceAccessCertificateBase64)
                .fullVehicleAccessCertificateBase64(fullVehicleAccessCertificateBase64)
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
