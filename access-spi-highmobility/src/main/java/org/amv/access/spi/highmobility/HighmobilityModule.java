package org.amv.access.spi.highmobility;

import com.google.common.base.Charsets;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.core.*;
import org.amv.access.core.impl.AccessCertificateImpl;
import org.amv.access.core.impl.DeviceCertificateImpl;
import org.amv.access.core.impl.IssuerImpl;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.CreateAccessCertificateRequest;
import org.amv.access.spi.CreateDeviceCertificateRequest;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer;
import org.amv.highmobility.cryptotool.CryptotoolWithIssuer.CertificateIssuer;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class HighmobilityModule implements AmvAccessModuleSpi {

    private final CryptotoolWithIssuer cryptotool;

    private final Issuer issuer;

    public HighmobilityModule(CryptotoolWithIssuer cryptotool) {
        this.cryptotool = requireNonNull(cryptotool);

        CertificateIssuer certificateIssuer = cryptotool.getCertificateIssuer();

        this.issuer = IssuerImpl.builder()
                .name(certificateIssuer.getName())
                .publicKeyBase64(base64OrThrow(certificateIssuer.getKeys().getPublicKey()))
                .build();
    }

    @Override
    public Mono<Boolean> isValidNonceAuth(NonceAuthentication nonceAuthentication, Device deviceEntity) {
        requireNonNull(nonceAuthentication);
        requireNonNull(deviceEntity);

        return cryptotool.verifySignature(nonceAuthentication.getNonce(),
                nonceAuthentication.getSignedNonce(),
                deviceEntity.getPublicKey())
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

        DeviceCertificate deviceCertificateEntity = DeviceCertificateImpl.builder()
                .issuer(this.issuer)
                .application(application)
                .device(device)
                .certificate(deviceCertificate.getDeviceCertificate())
                .signedCertificate(signedDeviceCertificate.getSignature())
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

        /*
        LocalDateTime validFrom = LocalDateTime.ofInstant(accessCertificateRequest
                        .getValidFrom()
                        .toInstant(),
                ZoneId.systemDefault());

        LocalDateTime validUntil = LocalDateTime.ofInstant(accessCertificateRequest
                        .getValidUntil()
                        .toInstant(),
                ZoneId.systemDefault());*/

        Cryptotool.AccessCertificate deviceAccessCertificate = cryptotool.createAccessCertificate(
                vehicle.getSerialNumber(),
                vehicle.getPublicKey(),
                device.getSerialNumber(),
                validFrom,
                validUntil)
                .block();

        Cryptotool.AccessCertificate vehicleAccessCertificate = cryptotool.createAccessCertificate(
                device.getSerialNumber(),
                device.getPublicKey(),
                vehicle.getSerialNumber(),
                validFrom,
                validUntil
        ).block();

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
                .uuid(UUID.randomUUID().toString())
                .application(application)
                .vehicle(vehicle)
                .device(device)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .signedVehicleAccessCertificateBase64(base64OrThrow(signedVehicleAccessCertificate))
                .signedDeviceAccessCertificateBase64(base64OrThrow(signedDeviceAccessCertificate))
                .build();

        return Mono.just(accessCertificateEntity);
    }

    private String base64OrThrow(String signedDeviceAccessCertificate) {
        return Optional.ofNullable(signedDeviceAccessCertificate)
                .map(s -> s.getBytes(Charsets.UTF_8))
                .map(s -> Base64.getEncoder().encodeToString(s))
                .orElseThrow(() -> new IllegalStateException("Error while encoding to base64"));
    }
}
