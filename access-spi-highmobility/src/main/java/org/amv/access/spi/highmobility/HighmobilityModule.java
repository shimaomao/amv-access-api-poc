package org.amv.access.spi.highmobility;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.*;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.core.impl.DeviceCertificateImpl;
import org.amv.access.core.impl.SignedAccessCertificateImpl;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.CreateAccessCertificateRequest;
import org.amv.access.spi.CreateDeviceCertificateRequest;
import org.amv.access.spi.SignCertificateRequest;
import org.amv.access.util.MoreBase64;
import org.amv.highmobility.cryptotool.Cryptotool;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.decodeBase64AsHex;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.encodeHexAsBase64;

@Slf4j
public class HighmobilityModule implements AmvAccessModuleSpi {

    private final Cryptotool cryptotool;
    private final SignatureService signatureService;

    public HighmobilityModule(Cryptotool cryptotool, SignatureService signatureService) {
        this.cryptotool = requireNonNull(cryptotool);
        this.signatureService = requireNonNull(signatureService);
    }

    @Override
    public Mono<Boolean> isValidNonceAuth(NonceAuthentication auth, String publicKeyBase64) {
        requireNonNull(auth);
        requireNonNull(publicKeyBase64);

        String devicePublicKey = decodeBase64AsHex(publicKeyBase64);
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

        String issuerPublicKeyBase64 = issuer.getPublicKeyBase64();
        String issuerPrivateKeyBase64 = issuer.getPrivateKeyBase64()
                .orElseThrow(() -> new IllegalArgumentException("Issuer private key is not present"));

        Cryptotool.DeviceCertificate hmDeviceCertificate = cryptotool
                .createDeviceCertificate(issuer.getNameInHex(),
                        application.getAppId(),
                        device.getSerialNumber(),
                        decodeBase64AsHex(device.getPublicKeyBase64()))
                .block();

        String deviceCertificateBase64 = encodeHexAsBase64(hmDeviceCertificate.getDeviceCertificate());

        String signatureBase64OrNull = signatureService.generateSignature(deviceCertificateBase64, issuerPrivateKeyBase64)
                .block();

        Optional.ofNullable(signatureBase64OrNull)
                .map(signatureBase64 -> signatureService.verifySignature(deviceCertificateBase64, signatureBase64, issuerPublicKeyBase64))
                .map(Mono::block)
                .orElseThrow(() -> new IllegalArgumentException("Could not verify device cert signature"));

        String signedDeviceCertificate = hmDeviceCertificate.getDeviceCertificate() + decodeBase64AsHex(signatureBase64OrNull);
        String signedDeviceCertificateBase64 = encodeHexAsBase64(signedDeviceCertificate);

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

        Device device = requireNonNull(accessCertificateRequest.getDevice());
        Vehicle vehicle = requireNonNull(accessCertificateRequest.getVehicle());
        Permissions permissions = requireNonNull(accessCertificateRequest.getPermissions());

        Instant validFrom = requireNonNull(accessCertificateRequest.getValidFrom());
        Instant validUntil = requireNonNull(accessCertificateRequest.getValidUntil());

        String vehiclePublicKey = decodeBase64AsHex(vehicle.getPublicKeyBase64());
        String devicePublicKey = decodeBase64AsHex(device.getPublicKeyBase64());

        Cryptotool.AccessCertificate deviceAccessCertificate = cryptotool.createAccessCertificate(
                vehicle.getSerialNumber(),
                vehiclePublicKey,
                device.getSerialNumber(),
                LocalDateTime.ofInstant(validFrom, ZoneOffset.UTC),
                LocalDateTime.ofInstant(validUntil, ZoneOffset.UTC),
                permissions.getPermissions()).block();

        Cryptotool.AccessCertificate vehicleAccessCertificate = cryptotool.createAccessCertificate(
                device.getSerialNumber(),
                devicePublicKey,
                vehicle.getSerialNumber(),
                LocalDateTime.ofInstant(validFrom, ZoneOffset.UTC),
                LocalDateTime.ofInstant(validUntil, ZoneOffset.UTC),
                permissions.getPermissions()).block();

        AccessCertificate accessCertificate = AccessCertificateImpl.builder()
                .deviceAccessCertificateBase64(encodeHexAsBase64(deviceAccessCertificate.getAccessCertificate()))
                .vehicleAccessCertificateBase64(encodeHexAsBase64(vehicleAccessCertificate.getAccessCertificate()))
                .build();

        return Mono.just(accessCertificate);
    }

    @Override
    public Mono<SignedAccessCertificate> signAccessCertificate(SignCertificateRequest signCertificateRequest) {
        requireNonNull(signCertificateRequest);

        AccessCertificate accessCertificate = signCertificateRequest.getAccessCertificate();
        String privateKeyBase64 = signCertificateRequest.getPrivateKeyBase64();

        String deviceAccessCertificateBase64 = accessCertificate.getDeviceAccessCertificateBase64();
        String deviceAccessCertSignatureBase64 = Optional.ofNullable(signatureService
                .generateSignature(deviceAccessCertificateBase64, privateKeyBase64))
                .map(Mono::block)
                .orElseThrow(() -> new IllegalStateException("Could not create device access cert signature"));

        String signedDeviceAccessCertBase64 = MoreBase64.encodeHexAsBase64(
                MoreBase64.decodeBase64AsHex(accessCertificate.getDeviceAccessCertificateBase64()) +
                        MoreBase64.decodeBase64AsHex(deviceAccessCertSignatureBase64));

        String vehicleAccessCertificateBase64 = accessCertificate.getVehicleAccessCertificateBase64();
        String vehicleAccessCertSignatureBase64 = Optional.ofNullable(signatureService
                .generateSignature(vehicleAccessCertificateBase64, privateKeyBase64))
                .map(Mono::block)
                .orElseThrow(() -> new IllegalStateException("Could not create vehicle access cert signature"));

        String signedVehicleAccessCertBase64 = MoreBase64.encodeHexAsBase64(
                MoreBase64.decodeBase64AsHex(accessCertificate.getVehicleAccessCertificateBase64()) +
                        MoreBase64.decodeBase64AsHex(vehicleAccessCertSignatureBase64));

        if (signCertificateRequest.getPublicKeyBase64().isPresent()) {
            String publicKeyBase64 = signCertificateRequest.getPublicKeyBase64().get();

            verifySignatureOrThrow(deviceAccessCertificateBase64,
                    deviceAccessCertSignatureBase64,
                    publicKeyBase64,
                    "device access certificate signature is invalid");

            verifySignatureOrThrow(vehicleAccessCertificateBase64,
                    vehicleAccessCertSignatureBase64,
                    publicKeyBase64,
                    "vehicle access certificate signature is invalid");
        }

        return Mono.just(SignedAccessCertificateImpl.builder()
                .deviceAccessCertificateBase64(deviceAccessCertificateBase64)
                .deviceAccessCertificateSignatureBase64(deviceAccessCertSignatureBase64)
                .signedDeviceAccessCertificateBase64(signedDeviceAccessCertBase64)
                .vehicleAccessCertificateBase64(vehicleAccessCertificateBase64)
                .vehicleAccessCertificateSignatureBase64(vehicleAccessCertSignatureBase64)
                .signedVehicleAccessCertificateBase64(signedVehicleAccessCertBase64)
                .build());
    }

    @Override
    public Mono<String> generateSignature(String messageBase64, String privateKeyBase64) {
        return signatureService.generateSignature(messageBase64, privateKeyBase64);
    }

    @Override
    public Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, String publicKeyBase64) {
        return signatureService.verifySignature(messageBase64, signatureBase64, publicKeyBase64);
    }


    private void verifySignatureOrThrow(String messageBase64,
                                        String signatureBase64,
                                        String publicKeyBase64,
                                        String errorMessage) {
        boolean isValidSignature = Optional.ofNullable(signatureService
                .verifySignature(messageBase64, signatureBase64, publicKeyBase64))
                .map(Mono::block)
                .orElse(false);

        if (!isValidSignature) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
