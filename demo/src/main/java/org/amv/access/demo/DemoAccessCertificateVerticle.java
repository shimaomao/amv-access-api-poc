package org.amv.access.demo;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.certificate.AccessCertificateService;
import org.amv.access.model.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DemoAccessCertificateVerticle extends AbstractVerticle {

    private final EventBus eventBus;
    private final DemoService demoService;
    private final DeviceRepository deviceRepository;
    private final AccessCertificateService accessCertificateService;

    public DemoAccessCertificateVerticle(EventBus eventBus,
                                         DemoService demoService,
                                         DeviceRepository deviceRepository,
                                         AccessCertificateService accessCertificateService) {
        this.eventBus = requireNonNull(eventBus);
        this.demoService = requireNonNull(demoService);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.accessCertificateService = requireNonNull(accessCertificateService);
    }

    @Override
    public void start() throws Exception {
        eventBus.consumer(DeviceCertificateEntity.class.getName(), (Handler<Message<String>>) event -> {
            vertx.executeBlocking(future -> {

                DeviceCertificateEntity deviceCertificateEntity = Json.decodeValue(event.body(), DeviceCertificateEntity.class);
                log.info("Got event {}: {}", event.address(), deviceCertificateEntity);

                ApplicationEntity demoApplication = demoService.getOrCreateDemoApplication();

                boolean isDeviceCertificateForDemoApplication = deviceCertificateEntity.getApplicationId() == demoApplication.getId();
                if (!isDeviceCertificateForDemoApplication) {
                    future.complete();
                    return;
                }

                DeviceEntity device = deviceRepository.findOne(deviceCertificateEntity.getDeviceId());
                if (device == null) {
                    String message = String.format("Could not find device %d of device certificate with id %d",
                            deviceCertificateEntity.getDeviceId(), deviceCertificateEntity.getId());
                    future.fail(new IllegalStateException(message));
                    return;
                }

                VehicleEntity demoVehicle = demoService.getOrCreateDemoVehicle();

                log.info("Creating demo access certificate for device {} and vehicle {}", device.getSerialNumber(), demoVehicle.getSerialNumber());

                accessCertificateService.createAccessCertificate(AccessCertificateService.CreateAccessCertificateContext.builder()
                        .appId(demoApplication.getAppId())
                        .deviceSerialNumber(device.getSerialNumber())
                        .vehicleSerialNumber(demoVehicle.getSerialNumber())
                        .validityStart(LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC))
                        .validityEnd(LocalDateTime.now().plusYears(2).toInstant(ZoneOffset.UTC))
                        .build())
                        .subscribe(future::complete, future::fail);
            }, result -> {
                if (result.succeeded()) {
                    log.info("Successfully created demo access certificate: {}", result.result());
                }
                if (result.failed()) {
                    log.error("", result.cause());
                }
            });
        });
    }
}
