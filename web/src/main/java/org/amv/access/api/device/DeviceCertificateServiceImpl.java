package org.amv.access.api.device;

import org.amv.access.model.*;
import org.amv.access.api.device.model.CreateDeviceCertificateRequest;
import org.amv.access.api.device.model.RevokeDeviceCertificateRequest;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.amv.highmobility.cryptotool.CryptotoolUtils.TestUtils;
import org.amv.access.spi.AmvAccessModuleSpi;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import static java.util.Objects.requireNonNull;

public class DeviceCertificateServiceImpl implements DeviceCertificateService {
    private final AmvAccessModuleSpi amvAccessModule;
    private final DeviceRepository deviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;
    private final DeviceCertificateRequestRepository deviceCertificateRequestRepository;

    public DeviceCertificateServiceImpl(AmvAccessModuleSpi amvAccessModule,
                                        DeviceRepository deviceRepository,
                                        DeviceCertificateRepository deviceCertificateRepository,
                                        DeviceCertificateRequestRepository deviceCertificateRequestRepository) {
        this.amvAccessModule = requireNonNull(amvAccessModule);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.deviceCertificateRepository = requireNonNull(deviceCertificateRepository);
        this.deviceCertificateRequestRepository = requireNonNull(deviceCertificateRequestRepository);
    }

    @Override
    public Mono<DeviceCertificate> createDeviceCertificate(CreateDeviceCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        return Mono.just(request)
                .map(this::saveCreateDeviceCertificateRequest)
                .map(deviceCertificateRequest ->  Tuples.of(deviceCertificateRequest, createAndSaveDevice(deviceCertificateRequest)))
                .flatMap(deviceCertificateRequestAndDevice -> amvAccessModule
                        .createDeviceCertificate(
                                deviceCertificateRequestAndDevice.getT1(),
                                deviceCertificateRequestAndDevice.getT2()
                                ))
                .map(deviceCertificateRepository::save)
                .single();
    }

    @Override
    public Mono<Void> revokeDeviceCertificate(RevokeDeviceCertificateRequest request) {
        // TODO: implement me
        throw new UnsupportedOperationException();
    }

    private DeviceCertificateRequest saveCreateDeviceCertificateRequest(CreateDeviceCertificateRequest request) {
        requireNonNull(request, "`request` must not be null");

        DeviceCertificateRequest deviceCertificateRequestEntity = DeviceCertificateRequest.builder()
                .appId(request.getAppId())
                .publicKey(request.getPublicKey())
                .name(request.getName())
                .build();

        return deviceCertificateRequestRepository.save(deviceCertificateRequestEntity);
    }

    private Device createAndSaveDevice(DeviceCertificateRequest deviceCertificateRequestEntity) {
        requireNonNull(deviceCertificateRequestEntity);
        requireNonNull(deviceCertificateRequestEntity.getPublicKey());

        String devicePublicKey = CryptotoolUtils.decodeBase64AsHex(deviceCertificateRequestEntity.getPublicKey());
        String deviceSerialNumberMock = TestUtils.generateRandomSerial();

        Device device = Device.builder()
                .appId(deviceCertificateRequestEntity.getAppId())
                .name(deviceCertificateRequestEntity.getName())
                .serialNumber(deviceSerialNumberMock)
                .publicKey(devicePublicKey)
                .build();

        return deviceRepository.save(device);
    }
}
