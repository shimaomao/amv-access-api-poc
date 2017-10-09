package org.amv.access.api.device;

import lombok.extern.slf4j.Slf4j;
import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.exception.BadRequestException;
import org.amv.access.exception.NotFoundException;
import org.amv.access.model.*;
import org.amv.access.spi.AmvAccessModuleSpi;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.amv.highmobility.cryptotool.CryptotoolUtils.TestUtils;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DeviceCertificateServiceImpl implements DeviceCertificateService {
    private final AmvAccessModuleSpi amvAccessModule;
    private final ApplicationRepository applicationRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;
    private final DeviceCertificateRequestRepository deviceCertificateRequestRepository;

    public DeviceCertificateServiceImpl(AmvAccessModuleSpi amvAccessModule,
                                        ApplicationRepository applicationRepository,
                                        DeviceRepository deviceRepository,
                                        DeviceCertificateRepository deviceCertificateRepository,
                                        DeviceCertificateRequestRepository deviceCertificateRequestRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.deviceCertificateRepository = requireNonNull(deviceCertificateRepository);
        this.deviceCertificateRequestRepository = requireNonNull(deviceCertificateRequestRepository);
    }

    @Override
    @Transactional
    public Mono<DeviceCertificate> createDeviceCertificate(CreateDeviceCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        Application application = applicationRepository.findOneByAppId(request.getAppId())
                .orElseThrow(() -> new NotFoundException("Application with given appId not found"));

        if (!application.isEnabled()) {
            throw new BadRequestException("Application with given appId is disabled");
        }

        if (log.isDebugEnabled()) {
            log.debug("Issuing device certificate for application {}", application);
        }

        DeviceCertificateRequest deviceCertificateRequest = saveCreateDeviceCertificateRequest(request);
        Device device = createAndSaveDevice(application, deviceCertificateRequest);

        DeviceCertificate deviceCertificate = amvAccessModule
                .createDeviceCertificate(application, device)
                .block();

        DeviceCertificate savedDeviceCertificate = deviceCertificateRepository.save(deviceCertificate);

        return Mono.justOrEmpty(savedDeviceCertificate);
    }

    private DeviceCertificateRequest saveCreateDeviceCertificateRequest(CreateDeviceCertificateRequest request) {
        requireNonNull(request);

        DeviceCertificateRequest deviceCertificateRequestEntity = DeviceCertificateRequest.builder()
                .appId(request.getAppId())
                .publicKey(request.getPublicKey())
                .name(request.getName())
                .build();

        return deviceCertificateRequestRepository.save(deviceCertificateRequestEntity);
    }

    private Device createAndSaveDevice(Application application, DeviceCertificateRequest deviceCertificateRequestEntity) {
        requireNonNull(application);
        requireNonNull(deviceCertificateRequestEntity);
        requireNonNull(deviceCertificateRequestEntity.getPublicKey());

        String devicePublicKey = CryptotoolUtils.decodeBase64AsHex(deviceCertificateRequestEntity.getPublicKey());
        String deviceSerialNumber = generateNewDeviceSerial();

        Device device = Device.builder()
                .appId(application.getAppId())
                .name(deviceCertificateRequestEntity.getName())
                .serialNumber(deviceSerialNumber)
                .publicKey(devicePublicKey)
                .build();

        return deviceRepository.save(device);
    }

    private String generateNewDeviceSerial() {
        int numberOfTries = 25;

        for (int i = 0; i < numberOfTries; i++) {
            String deviceSerialNumber = TestUtils.generateRandomSerial();

            Optional<Device> bySerialNumber = deviceRepository.findBySerialNumber(deviceSerialNumber);
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
