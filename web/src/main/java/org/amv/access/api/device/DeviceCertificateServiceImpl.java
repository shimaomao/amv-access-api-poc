package org.amv.access.api.device;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.core.Issuer;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.model.CreateDeviceCertificateRequestImpl;
import org.amv.access.util.SecureRandomUtils;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DeviceCertificateServiceImpl implements DeviceCertificateService {
    private final AmvAccessModuleSpi amvAccessModule;
    private final IssuerRepository issuerRepository;
    private final ApplicationRepository applicationRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;
    private final DeviceCertificateRequestRepository deviceCertificateRequestRepository;

    public DeviceCertificateServiceImpl(AmvAccessModuleSpi amvAccessModule,
                                        IssuerRepository issuerRepository,
                                        ApplicationRepository applicationRepository,
                                        DeviceRepository deviceRepository,
                                        DeviceCertificateRepository deviceCertificateRepository,
                                        DeviceCertificateRequestRepository deviceCertificateRequestRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.issuerRepository = requireNonNull(issuerRepository);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.deviceCertificateRepository = requireNonNull(deviceCertificateRepository);
        this.deviceCertificateRequestRepository = requireNonNull(deviceCertificateRequestRepository);
    }

    @Override
    @Transactional
    public Mono<DeviceCertificate> createDeviceCertificate(ApplicationAuthentication auth, CreateDeviceCertificateRequest request) {
        requireNonNull(auth, "`auth` must not be null");
        requireNonNull(request, "`request` must not be null");

        ApplicationEntity application = findApplicationOrThrow(auth);
        DeviceCertificateRequest deviceCertificateRequest = saveCreateDeviceCertificateRequest(request);
        DeviceEntity device = createAndSaveDevice(application, deviceCertificateRequest);

        if (log.isDebugEnabled()) {
            log.debug("Issuing device certificate for application {}", application);
            log.debug("Device {} created", device);
        }

        DeviceCertificate deviceCertificate = amvAccessModule
                .createDeviceCertificate(CreateDeviceCertificateRequestImpl.builder()
                        .application(application)
                        .device(device)
                        .build())
                .block();

        IssuerEntity issuerEntity = findIssuerOrCreateIfNecessary(deviceCertificate.getIssuer());

        DeviceCertificateEntity deviceCertificateEntity = DeviceCertificateEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .applicationId(application.getId())
                .issuerId(issuerEntity.getId())
                .deviceId(device.getId())
                .certificateBase64(deviceCertificate.getCertificateBase64())
                .signedCertificateBase64(deviceCertificate.getSignedCertificateBase64())
                .build();

        deviceCertificateRepository.save(deviceCertificateEntity);

        return Mono.justOrEmpty(deviceCertificate);
    }

    private ApplicationEntity findApplicationOrThrow(ApplicationAuthentication auth) {
        ApplicationEntity application = applicationRepository.findOneByAppId(auth.getApplication().getAppId())
                .orElseThrow(() -> new NotFoundException("ApplicationEntity with given appId not found"));

        if (!application.isEnabled()) {
            throw new BadRequestException("ApplicationEntity with given appId is disabled");
        }

        return application;
    }


    private DeviceCertificateRequest saveCreateDeviceCertificateRequest(CreateDeviceCertificateRequest request) {
        requireNonNull(request);

        DeviceCertificateRequest deviceCertificateRequestEntity = DeviceCertificateRequest.builder()
                .appId(request.getAppId())
                .publicKeyBase64(request.getDevicePublicKeyBase64())
                .name(request.getDeviceName())
                .build();

        return deviceCertificateRequestRepository.save(deviceCertificateRequestEntity);
    }

    private DeviceEntity createAndSaveDevice(ApplicationEntity application, DeviceCertificateRequest deviceCertificateRequestEntity) {
        requireNonNull(application);
        requireNonNull(deviceCertificateRequestEntity);
        String publicKeyBase64 = requireNonNull(deviceCertificateRequestEntity.getPublicKeyBase64());

        String deviceSerialNumber = generateNewDeviceSerial();

        DeviceEntity device = DeviceEntity.builder()
                .applicationId(application.getId())
                .name(deviceCertificateRequestEntity.getName())
                .serialNumber(deviceSerialNumber)
                .publicKeyBase64(publicKeyBase64)
                .build();

        return deviceRepository.save(device);
    }

    private String generateNewDeviceSerial() {
        int numberOfTries = 25;

        for (int i = 0; i < numberOfTries; i++) {
            String deviceSerialNumber = SecureRandomUtils.generateRandomSerial();

            Optional<DeviceEntity> bySerialNumber = deviceRepository.findBySerialNumber(deviceSerialNumber);
            boolean deviceWithSerialNumberAlreadyExists = bySerialNumber.isPresent();
            if (deviceWithSerialNumberAlreadyExists) {
                log.warn("Could not obtain new device serial number on {}. try...", i + 1);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Obtained new device serial number: {}", deviceSerialNumber);
                }
                return deviceSerialNumber;
            }
        }

        String errorMessage = String.format("Could not obtain a device serial after %d retries", numberOfTries);
        throw new IllegalStateException(errorMessage);
    }

    // TODO: currently an issuer must be created on demand - should be created on application start
    private IssuerEntity findIssuerOrCreateIfNecessary(Issuer issuer) {
        requireNonNull(issuer);

        try {
            return issuerRepository
                    .findByNameAndPublicKeyBase64(issuer.getName(), issuer.getPublicKeyBase64())
                    .orElseThrow(() -> new IllegalStateException("Could not find issuer"));
        } catch (IllegalStateException e) {
            log.warn("Issuer '{}' will be created as it does not yet exist.", issuer.getName());
            return issuerRepository.save(IssuerEntity.builder()
                    .name(issuer.getName())
                    .publicKeyBase64(issuer.getPublicKeyBase64())
                    .build());
        }
    }
}
