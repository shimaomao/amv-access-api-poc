package org.amv.access.demo;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.AmvAccessApplication;
import org.amv.access.model.*;
import org.amv.access.util.SecureRandomUtils;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DemoService {
    private static final String DEMO_ISSUER_NAME = "demo";
    private static final String DEMO_USER_NAME = "demo";
    private static final String DEMO_USER_PASSWORD = "demodemodemo";
    private static final String DEMO_APP_NAME = "demo";
    private static final String DEMO_APP_API_KEY = "demodemodemo";

    private final Cryptotool cryptotool;
    private final PasswordEncoder passwordEncoder;
    private final IssuerRepository issuerRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;

    private final Supplier<DemoUser.DemoUserBuilder> demoUserBuilderSupplier = Suppliers
            .memoize(this::createDemoUserBuilder);

    public DemoService(Cryptotool cryptotool,
                       PasswordEncoder passwordEncoder,
                       IssuerRepository issuerRepository,
                       ApplicationRepository applicationRepository,
                       UserRepository userRepository,
                       VehicleRepository vehicleRepository,
                       DeviceRepository deviceRepository) {
        this.cryptotool = cryptotool;
        this.passwordEncoder = requireNonNull(passwordEncoder);
        this.issuerRepository = requireNonNull(issuerRepository);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.userRepository = requireNonNull(userRepository);
        this.vehicleRepository = requireNonNull(vehicleRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
    }

    public DemoUser getOrCreateDemoUser() {
        final UserEntity user = userRepository.findByName(DEMO_USER_NAME, AmvAccessApplication.standardPageRequest)
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(() -> createDemoUser().getOrigin());

        return demoUserBuilderSupplier.get()
                .origin(user)
                .build();
    }

    public IssuerEntity getOrCreateDemoIssuer() {
        return issuerRepository.findByName(DEMO_ISSUER_NAME)
                .stream()
                .findFirst()
                .orElseGet(this::createDemoIssuer);
    }

    public ApplicationEntity getOrCreateDemoApplication() {
        return applicationRepository.findByName(DEMO_APP_NAME, AmvAccessApplication.standardPageRequest)
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(this::createDemoApplication);
    }

    public DeviceEntity createDemoDevice(ApplicationEntity applicationEntity) {
        return createDemoDevice(getOrCreateDemoIssuer(), applicationEntity);
    }

    public VehicleEntity createDemoVehicle() {
        return createDemoVehicle(getOrCreateDemoIssuer());
    }

    public void createDemoData() {
        IssuerEntity demoIssuer = this.getOrCreateDemoIssuer();
        ApplicationEntity demoApplication = this.getOrCreateDemoApplication();

        this.createDemoVehicle(demoIssuer);
        this.createDemoDevice(demoIssuer, demoApplication);

        this.getOrCreateDemoUser();
    }

    private DemoUser createDemoUser() {
        final DemoUser.DemoUserBuilder demoUserBuilder = demoUserBuilderSupplier.get();
        final ArrayList<String> authorities = Lists.newArrayList("ROLE_ADMIN", "ROLE_USER");

        UserEntity demoUser = UserEntity.builder()
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

    private IssuerEntity createDemoIssuer() {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        IssuerEntity demoIssuer = IssuerEntity.builder()
                .name(DEMO_ISSUER_NAME)
                .created(Date.from(Instant.EPOCH))
                .publicKeyBase64(CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey()))
                .privateKeyBase64(CryptotoolUtils.encodeHexAsBase64(keys.getPrivateKey()))
                .build();

        IssuerEntity savedDemoIssuer = issuerRepository.save(demoIssuer);

        log.info("Created demo issuer: {}", savedDemoIssuer);

        return savedDemoIssuer;

    }

    private DemoUser.DemoUserBuilder createDemoUserBuilder() {
        final String password = DEMO_USER_PASSWORD;
        return DemoUser.builder()
                .name(DEMO_USER_NAME)
                .password(password)
                .encryptedPassword(passwordEncoder.encode(password));
    }

    private ApplicationEntity createDemoApplication() {
        ApplicationEntity application = ApplicationEntity.builder()
                .name(DEMO_APP_NAME)
                .appId(SecureRandomUtils.generateRandomAppId())
                .apiKey(DEMO_APP_API_KEY)
                .enabled(true)
                .build();

        ApplicationEntity savedDemoApplication = applicationRepository.save(application);

        log.info("Created demo application: {}", savedDemoApplication);

        return savedDemoApplication;
    }

    private VehicleEntity createDemoVehicle(IssuerEntity demoIssuer) {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        VehicleEntity demoVehicle = VehicleEntity.builder()
                .issuerId(demoIssuer.getId())
                .name(StringUtils.prependIfMissing(RandomStringUtils.randomAlphanumeric(10), "demo-vehicle-"))
                .serialNumber(SecureRandomUtils.generateRandomSerial())
                .publicKeyBase64(publicKeyBase64)
                .build();

        final VehicleEntity savedDemoVehicle = vehicleRepository.save(demoVehicle);

        log.info("Created demo vehicle: {}", savedDemoVehicle);

        return savedDemoVehicle;
    }

    private DeviceEntity createDemoDevice(IssuerEntity issuerEntity, ApplicationEntity applicationEntity) {
        DeviceWithKeys demoDeviceWithKeys = createDemoDeviceWithKeys(issuerEntity, applicationEntity);

        return demoDeviceWithKeys.getDevice();
    }

    public DeviceWithKeys createDemoDeviceWithKeys(IssuerEntity issuerEntity, ApplicationEntity applicationEntity) {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        String publicKeyBase64 = CryptotoolUtils.encodeHexAsBase64(keys.getPublicKey());

        DeviceEntity demoDevice = DeviceEntity.builder()
                .issuerId(issuerEntity.getId())
                .applicationId(applicationEntity.getId())
                .name(StringUtils.prependIfMissing(RandomStringUtils.randomAlphanumeric(10), "demo-device-"))
                .serialNumber(SecureRandomUtils.generateRandomSerial())
                .publicKeyBase64(publicKeyBase64)
                .build();

        final DeviceEntity savedDemoDevice = deviceRepository.save(demoDevice);

        log.info("Created demo device: {}", savedDemoDevice);

        return DeviceWithKeys.builder()
                .device(savedDemoDevice)
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
