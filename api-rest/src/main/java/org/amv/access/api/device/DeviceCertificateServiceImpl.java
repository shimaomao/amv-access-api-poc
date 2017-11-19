package org.amv.access.api.device;

import io.vertx.core.json.Json;
import io.vertx.rxjava.core.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.ApplicationAuthentication;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.certificate.DeviceCertificateService;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.issuer.IssuerService;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.access.spi.model.CreateDeviceCertificateRequestImpl;
import org.amv.highmobility.cryptotool.CryptotoolUtils.SecureRandomUtils;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DeviceCertificateServiceImpl implements DeviceCertificateService {

    private final AmvAccessModuleSpi amvAccessModule;
    private final EventBus eventBus;
    private final IssuerService issuerService;
    private final ApplicationRepository applicationRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;

    public DeviceCertificateServiceImpl(AmvAccessModuleSpi amvAccessModule,
                                        EventBus eventBus,
                                        IssuerService issuerService,
                                        ApplicationRepository applicationRepository,
                                        DeviceRepository deviceRepository,
                                        DeviceCertificateRepository deviceCertificateRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.eventBus = requireNonNull(eventBus);
        this.issuerService = requireNonNull(issuerService);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.deviceCertificateRepository = requireNonNull(deviceCertificateRepository);
    }

    @Override
    @Transactional
    public Mono<DeviceCertificate> createDeviceCertificate(ApplicationAuthentication auth, CreateDeviceCertificateContext request) {
        requireNonNull(auth, "`auth` must not be null");
        requireNonNull(request, "`request` must not be null");

        ApplicationEntity application = findApplicationOrThrow(auth);

        IssuerEntity issuer = issuerService.findActiveIssuerOrThrow();

        DeviceEntity device = createAndSaveDevice(request);

        if (log.isDebugEnabled()) {
            log.debug("Issuing device certificate for application {}", application);
            log.debug("Device {} created", device);
        }

        DeviceCertificate deviceCertificate = amvAccessModule
                .createDeviceCertificate(CreateDeviceCertificateRequestImpl.builder()
                        .issuer(issuer)
                        .application(application)
                        .device(device)
                        .build())
                .block();

        DeviceCertificateEntity deviceCertificateEntity = DeviceCertificateEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .applicationId(application.getId())
                .issuerId(issuer.getId())
                .deviceId(device.getId())
                //.certificateBase64(deviceCertificate.getCertificateBase64())
                //.certificateSignatureBase64(deviceCertificate.getCertificateSignatureBase64())
                .signedCertificateBase64(deviceCertificate.getSignedDeviceCertificateBase64())
                .build();

        deviceCertificateRepository.save(deviceCertificateEntity);

        return Mono.justOrEmpty(deviceCertificate)
                .doOnSuccess(entity -> {
                    // TODO: not working correctly - event is sent asynchronously and does not work in tests atm
                    String eventName = entity.getClass().getName();
                    log.debug("Sending event {}", eventName);
                    this.eventBus.publisher(eventName)
                            .end(Json.encode(entity));
                });
    }

    private ApplicationEntity findApplicationOrThrow(ApplicationAuthentication auth) {
        ApplicationEntity application = applicationRepository.findOneByAppId(auth.getApplication().getAppId())
                .orElseThrow(() -> new NotFoundException("ApplicationEntity with given appId not found"));

        if (!application.isEnabled()) {
            throw new BadRequestException("ApplicationEntity with given appId is disabled");
        }

        return application;
    }

    private DeviceEntity createAndSaveDevice(CreateDeviceCertificateContext request) {
        requireNonNull(request);
        String publicKeyBase64 = requireNonNull(request.getDevicePublicKeyBase64());

        String deviceSerialNumber = generateNewDeviceSerial();

        DeviceEntity device = DeviceEntity.builder()
                .name(request.getDeviceName())
                .serialNumber(deviceSerialNumber)
                .publicKeyBase64(publicKeyBase64)
                .build();

        return deviceRepository.save(device);
    }

    private String generateNewDeviceSerial() {
        int numberOfTries = 25;

        for (int i = 0; i < numberOfTries; i++) {
            String deviceSerialNumber = SecureRandomUtils.generateRandomSerial()
                    .toLowerCase();

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
