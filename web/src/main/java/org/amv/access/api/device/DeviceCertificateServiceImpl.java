package org.amv.access.api.device;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.issuer.IssuerService;
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
    private final IssuerService issuerService;
    private final ApplicationRepository applicationRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;
    private final DeviceCertificateRequestRepository deviceCertificateRequestRepository;

    public DeviceCertificateServiceImpl(AmvAccessModuleSpi amvAccessModule,
                                        IssuerService issuerService,
                                        ApplicationRepository applicationRepository,
                                        DeviceRepository deviceRepository,
                                        DeviceCertificateRepository deviceCertificateRepository,
                                        DeviceCertificateRequestRepository deviceCertificateRequestRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.issuerService = requireNonNull(issuerService);
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

        IssuerEntity issuerEntity = issuerService.findActiveIssuerOrThrow();

        DeviceCertificateRequestEntity deviceCertificateRequest = saveCreateDeviceCertificateRequest(request);
        DeviceEntity device = createAndSaveDevice(application, deviceCertificateRequest);

        if (log.isDebugEnabled()) {
            log.debug("Issuing device certificate for application {}", application);
            log.debug("Device {} created", device);
        }

        DeviceCertificate deviceCertificate = amvAccessModule
                .createDeviceCertificate(CreateDeviceCertificateRequestImpl.builder()
                        .issuer(issuerEntity)
                        .application(application)
                        .device(device)
                        .build())
                .block();

        DeviceCertificateEntity deviceCertificateEntity = DeviceCertificateEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .issuerId(issuerEntity.getId())
                .applicationId(application.getId())
                .deviceId(device.getId())
                .certificateBase64(deviceCertificate.getCertificateBase64())
                .certificateSignatureBase64(deviceCertificate.getCertificateSignatureBase64())
                .fullCertificateBase64(deviceCertificate.getFullDeviceCertificateBase64())
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


    private DeviceCertificateRequestEntity saveCreateDeviceCertificateRequest(CreateDeviceCertificateRequest request) {
        requireNonNull(request);

        DeviceCertificateRequestEntity deviceCertificateRequestEntity = DeviceCertificateRequestEntity.builder()
                .appId(request.getAppId())
                .publicKeyBase64(request.getDevicePublicKeyBase64())
                .name(request.getDeviceName())
                .build();

        return deviceCertificateRequestRepository.save(deviceCertificateRequestEntity);
    }

    private DeviceEntity createAndSaveDevice(ApplicationEntity application, DeviceCertificateRequestEntity deviceCertificateRequestEntity) {
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

}
