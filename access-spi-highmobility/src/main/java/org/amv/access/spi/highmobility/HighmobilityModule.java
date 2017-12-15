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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.decodeBase64AsHex;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.encodeHexAsBase64;

@Slf4j
public class HighmobilityModule implements AmvAccessModuleSpi {

    private final Cryptotool cryptotool;

    public HighmobilityModule(Cryptotool cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);
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

        String issuerPrivateKeyBase64 = issuer.getPrivateKeyBase64()
                .orElseThrow(() -> new IllegalArgumentException("Issuer private key is not present"));

        Cryptotool.DeviceCertificate hmDeviceCertificate = cryptotool
                .createDeviceCertificate(issuer.getNameInHex(),
                        application.getAppId(),
                        device.getSerialNumber(),
                        decodeBase64AsHex(device.getPublicKeyBase64()))
                .block();

        String hmDeviceCertificateBase64 = encodeHexAsBase64(hmDeviceCertificate.getDeviceCertificate());

        String hmSignatureBase64 = this.createSignature(hmDeviceCertificateBase64, issuerPrivateKeyBase64)
                .block();

        String signedDeviceCertificate = hmDeviceCertificate.getDeviceCertificate() + decodeBase64AsHex(hmSignatureBase64);
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
                .uuid(UUID.randomUUID().toString())
                .name(vehicle.getName())
                .deviceAccessCertificateBase64(encodeHexAsBase64(deviceAccessCertificate.getAccessCertificate()))
                .vehicleAccessCertificateBase64(encodeHexAsBase64(vehicleAccessCertificate.getAccessCertificate()))
                .build();

        return Mono.just(accessCertificate);
    }

    @Override
    public Mono<String> createSignature(String messageBase64, String privateKeyBase64) {
        String messageInHex = decodeBase64AsHex(messageBase64);
        String issuerPrivateKeyInHex = decodeBase64AsHex(privateKeyBase64);

        return cryptotool.generateSignature(messageInHex, issuerPrivateKeyInHex)
                .map(Cryptotool.Signature::getSignature)
                .map(CryptotoolUtils::encodeHexAsBase64);
    }

    @Override
    public Mono<Boolean> verifySignature(String messageBase64, String signatureBase64, String publicKeyBase64) {
        String messageInHex = decodeBase64AsHex(messageBase64);
        String signatureInHex = decodeBase64AsHex(signatureBase64);
        String publicKeyInHex = decodeBase64AsHex(publicKeyBase64);

        return cryptotool.verifySignature(messageInHex, signatureInHex, publicKeyInHex)
                .map(s -> s == Cryptotool.Validity.VALID);
    }
}
