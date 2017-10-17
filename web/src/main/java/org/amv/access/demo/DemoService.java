package org.amv.access.demo;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.AmvAccessApplication;
import org.amv.access.model.*;
import org.amv.access.util.MoreBase64;
import org.amv.access.util.SecureRandomUtils;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DemoService {
    private static final String DEMO_USER_NAME = "demo";
    private static final String DEMO_USER_PASSWORD = "demodemodemo";
    private static final String DEMO_APP_NAME = "demo";
    private static final String DEMO_APP_API_KEY = "demodemodemo";

    private final Cryptotool cryptotool;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;

    private final Supplier<DemoUser.DemoUserBuilder> demoUserBuilderSupplier = Suppliers
            .memoize(this::createDemoUserBuilder);

    public DemoService(Cryptotool cryptotool,
                       PasswordEncoder passwordEncoder,
                       ApplicationRepository applicationRepository,
                       UserRepository userRepository,
                       VehicleRepository vehicleRepository,
                       DeviceRepository deviceRepository) {
        this.cryptotool = cryptotool;
        this.passwordEncoder = requireNonNull(passwordEncoder);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.userRepository = requireNonNull(userRepository);
        this.vehicleRepository = requireNonNull(vehicleRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
    }

    public DemoUser getOrCreateDemoUser() {
        final User user = userRepository.findByName(DEMO_USER_NAME, AmvAccessApplication.standardPageRequest)
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(() -> createDemoUser().getOrigin());

        return demoUserBuilderSupplier.get()
                .origin(user)
                .build();
    }

    public ApplicationEntity getOrCreateDemoApplication() {
        ApplicationEntity application = applicationRepository.findByName(DEMO_APP_NAME, AmvAccessApplication.standardPageRequest)
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(this::createDemoApplication);

        return applicationRepository.save(application);
    }

    public void createDemoData() {
        this.getOrCreateDemoUser();
        this.createDemoVehicle();

        ApplicationEntity demoApplication = this.getOrCreateDemoApplication();

        this.createDemoDevice(demoApplication);
    }

    private DemoUser createDemoUser() {
        final DemoUser.DemoUserBuilder demoUserBuilder = demoUserBuilderSupplier.get();
        final ArrayList<String> authorities = Lists.newArrayList("ROLE_ADMIN", "ROLE_USER");

        User demoUser = User.builder()
                .name(demoUserBuilder.name())
                .password(demoUserBuilder.encryptedPassword())
                .authorities(authorities)
                .build();

        userRepository.save(demoUser);
        log.info("Created demo user: {}", demoUser);

        return demoUserBuilder
                .origin(demoUser)
                .build();
    }

    private DemoUser.DemoUserBuilder createDemoUserBuilder() {
        final String password = DEMO_USER_PASSWORD;
        return DemoUser.builder()
                .name(DEMO_USER_NAME)
                .password(password)
                .encryptedPassword(passwordEncoder.encode(password));
    }

    private ApplicationEntity createDemoApplication() {
        return ApplicationEntity.builder()
                .name(DEMO_APP_NAME)
                .appId(SecureRandomUtils.generateRandomAppId())
                .apiKey(DEMO_APP_API_KEY)
                .enabled(true)
                .build();
    }

    public VehicleEntity createDemoVehicle() {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        String publicKeyBase64 = MoreBase64.toBase64(keys.getPublicKey())
                .orElseThrow(IllegalStateException::new);

        VehicleEntity vehicle = VehicleEntity.builder()
                .name(StringUtils.prependIfMissing(RandomStringUtils.randomAlphanumeric(10), "demo-vehicle-"))
                .serialNumber(SecureRandomUtils.generateRandomSerial())
                .publicKeyBase64(publicKeyBase64)
                .build();

        return vehicleRepository.save(vehicle);
    }


    public DeviceEntity createDemoDevice(ApplicationEntity applicationEntity) {
        DeviceWithKeys demoDeviceWithKeys = createDemoDeviceWithKeys(applicationEntity);

        return demoDeviceWithKeys.getDevice();
    }

    public DeviceWithKeys createDemoDeviceWithKeys(ApplicationEntity applicationEntity) {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        String publicKeyBase64 = MoreBase64.toBase64(keys.getPublicKey())
                .orElseThrow(IllegalStateException::new);

        DeviceEntity device = DeviceEntity.builder()
                .applicationId(applicationEntity.getId())
                .name(StringUtils.prependIfMissing(RandomStringUtils.randomAlphanumeric(10), "demo-device-"))
                .serialNumber(SecureRandomUtils.generateRandomSerial())
                .publicKeyBase64(publicKeyBase64)
                .build();

        return DeviceWithKeys.builder()
                .device(deviceRepository.save(device))
                .keys(keys)
                .build();
    }

    @Value
    @Builder
    public static class DeviceWithKeys {
        private DeviceEntity device;
        private Cryptotool.Keys keys;
    }
}
