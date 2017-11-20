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

        Cryptotool.DeviceCertificate hmDeviceCertificate = cryptotool
                .createDeviceCertificate(issuer.getNameInHex(),
                        application.getAppId(),
                        device.getSerialNumber(),
                        decodeBase64AsHex(device.getPublicKeyBase64()))
                .block();

        Cryptotool.Signature hmSignature = cryptotool
                .generateSignature(hmDeviceCertificate.getDeviceCertificate(), decodeBase64AsHex(issuer.getPrivateKeyBase64()))
                .block();

        String signedDeviceCertificate = hmDeviceCertificate.getDeviceCertificate() + hmSignature.getSignature();
        String signedDeviceCertificateBase64 = hexToBase64(signedDeviceCertificate)
                .orElseThrow(() -> new IllegalStateException("Could not convert signed device certificate to base64"));

        DeviceCertificate deviceCertificate = DeviceCertificateImpl.builder()
                .issuer(issuer)
                .application(application)
                .device(device)
                .signedDeviceCertificateBase64(signedDeviceCertificateBase64)
                .build();

        return Mono.just(deviceCertificate);
    }

    @Override
    public Mono<AccessCertificate> createAccessCertificate(CreateAccessCertificateRequest accessCertificateRequest) {
        requireNonNull(accessCertificateRequest);

        Issuer issuer = requireNonNull(accessCertificateRequest.getIssuer());
        Application application = requireNonNull(accessCertificateRequest.getApplication());
        Device device = requireNonNull(accessCertificateRequest.getDevice());
        Vehicle vehicle = requireNonNull(accessCertificateRequest.getVehicle());
        Permissions permissions = requireNonNull(accessCertificateRequest.getPermissions());

        LocalDateTime validFrom = requireNonNull(accessCertificateRequest.getValidFrom());
        LocalDateTime validUntil = requireNonNull(accessCertificateRequest.getValidUntil());

        String vehiclePublicKey = CryptotoolUtils.decodeBase64AsHex(vehicle.getPublicKeyBase64());
        String devicePublicKey = CryptotoolUtils.decodeBase64AsHex(device.getPublicKeyBase64());

        Cryptotool.AccessCertificate deviceAccessCertificate = cryptotool.createAccessCertificate(
                vehicle.getSerialNumber(),
                vehiclePublicKey,
                device.getSerialNumber(),
                validFrom,
                validUntil,
                permissions.getPermissions()).block();

        Cryptotool.AccessCertificate vehicleAccessCertificate = cryptotool.createAccessCertificate(
                device.getSerialNumber(),
                devicePublicKey,
                vehicle.getSerialNumber(),
                validFrom,
                validUntil,
                permissions.getPermissions()).block();

        String issuerPrivateKeyInHex = decodeBase64AsHex(issuer.getPrivateKeyBase64());
        String deviceAccessCertificateSignature = Mono.just(deviceAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cert -> cryptotool.generateSignature(cert, issuerPrivateKeyInHex))
                .map(Cryptotool.Signature::getSignature)
                .single()
                .block();

        String vehicleAccessCertificateSignature = Mono.just(vehicleAccessCertificate)
                .map(Cryptotool.AccessCertificate::getAccessCertificate)
                .flatMapMany(cert -> cryptotool.generateSignature(cert, issuerPrivateKeyInHex))
                .map(Cryptotool.Signature::getSignature)
                .single()
                .block();

        String signedDeviceAccessCertificate = deviceAccessCertificate.getAccessCertificate() + deviceAccessCertificateSignature;
        String signedDeviceAccessCertificateBase64 = hexToBase64(signedDeviceAccessCertificate)
                .orElseThrow(() -> new IllegalStateException("Could not convert signed device access certificate to base64"));

        String signedVehicleAccessCertificate = vehicleAccessCertificate.getAccessCertificate() + vehicleAccessCertificateSignature;
        String signedVehicleAccessCertificateBase64 = hexToBase64(signedVehicleAccessCertificate)
                .orElseThrow(() -> new IllegalStateException("Could not convert signed vehicle access certificate to base64"));

        AccessCertificate accessCertificateEntity = AccessCertificateImpl.builder()
                .uuid(UUID.randomUUID().toString())
                .issuer(issuer)
                .application(application)
                .vehicle(vehicle)
                .device(device)
                .signedDeviceAccessCertificateBase64(signedDeviceAccessCertificateBase64)
                .signedVehicleAccessCertificateBase64(signedVehicleAccessCertificateBase64)
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
