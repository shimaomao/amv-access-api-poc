package org.amv.access.demo;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.model.*;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils.SecureRandomUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.decodeBase64AsHex;
import static org.amv.highmobility.cryptotool.CryptotoolUtils.encodeHexAsBase64;

@Slf4j
@Transactional
public class DemoServiceImpl implements DemoService {
    private static final String DEMO_ISSUER_NAME = "demo";
    private static final String DEMO_USER_NAME = "access-demo-user";
    private static final String DEMO_USER_PASSWORD = "demodemodemo";
    private static final String DEMO_APP_NAME = "access-demo-application";
    private static final String DEMO_APP_API_KEY = "demodemodemo";
    private static final String DEMO_VEHICLE_NAME = "access-demo-verticle";
    private static final String DEMO_DEVICE_NAME = "access-demo-device";

    private final Cryptotool cryptotool;
    private final PasswordEncoder passwordEncoder;
    private final IssuerRepository issuerRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceRepository deviceRepository;

    private final Supplier<DemoUser.DemoUserBuilder> demoUserBuilderSupplier = Suppliers
            .memoize(this::createDemoUserBuilder);

    public DemoServiceImpl(Cryptotool cryptotool,
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

    @Override
    public void createDemoDataFromProperties(DemoProperties demoProperties) {
        this.getOrCreateDemoUser();
        this.getOrCreateDemoApplication();
        this.getOrCreateDemoDevice();
        this.getOrCreateDemoVehicle();

        IssuerEntity demoIssuer = demoProperties.getIssuer()
                .map(issuer -> {
                    if (DEMO_ISSUER_NAME.equals(issuer.getName())) {
                        throw new IllegalStateException("Your issuer is not allowed to be named '" + DEMO_ISSUER_NAME + "'");
                    }
                    return issuer;
                })
                .map(issuer -> issuerRepository.findByName(issuer.getName(), new PageRequest(0, 1))
                        .getContent()
                        .stream()
                        .findFirst()
                        .orElseGet(() -> this.createDemoIssuer(issuer)))
                .orElseThrow(() -> new IllegalStateException("Could not find or create demo issuer from properties file"));

        demoProperties.getVehicles().stream()
                .filter(v -> !vehicleRepository.findOneBySerialNumber(v.getSerialNumber()).isPresent())
                .forEach(vehicle -> this.createDemoVehicle(demoIssuer, vehicle));

        demoProperties.getApplications().stream()
                .filter(app -> !applicationRepository.findOneByAppId(app.getAppId()).isPresent())
                .forEach(this::createDemoApplication);
    }

    @Override
    public DeviceWithKeys createDemoDeviceWithKeys(ApplicationEntity applicationEntity) {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        String publicKeyBase64 = encodeHexAsBase64(keys.getPublicKey());

        DeviceEntity demoDevice = DeviceEntity.builder()
                .applicationId(applicationEntity.getId())
                .name(DEMO_DEVICE_NAME)
                .serialNumber(SecureRandomUtils.generateRandomSerial().toLowerCase())
                .publicKeyBase64(publicKeyBase64)
                .build();

        DeviceEntity savedDemoDevice = deviceRepository.save(demoDevice);

        log.info("Created demo device: {}", savedDemoDevice);

        return DeviceWithKeys.builder()
                .device(savedDemoDevice)
                .keys(keys)
                .build();
    }

    @Override
    public DemoUser getOrCreateDemoUser() {
        UserEntity user = userRepository.findByName(DEMO_USER_NAME, new PageRequest(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(() -> createDemoUser().getOrigin());

        return demoUserBuilderSupplier.get()
                .origin(user)
                .build();
    }

    @Override
    public IssuerEntity getOrCreateDemoIssuer() {
        return issuerRepository.findByName(DEMO_ISSUER_NAME, new PageRequest(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(this::createDemoIssuer);
    }

    public ApplicationEntity getOrCreateDemoApplication() {
        return applicationRepository.findByName(DEMO_APP_NAME, new PageRequest(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(this::createDemoApplication);
    }

    @Override
    public VehicleEntity getOrCreateDemoVehicle() {
        return vehicleRepository.findByName(DEMO_VEHICLE_NAME, new PageRequest(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(this::createDemoVehicle);
    }

    @Override
    public DeviceEntity getOrCreateDemoDevice() {
        return deviceRepository.findByName(DEMO_DEVICE_NAME, new PageRequest(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(this::createDemoDevice);
    }

    private VehicleEntity createDemoVehicle() {
        return createDemoVehicle(getOrCreateDemoIssuer());
    }

    private DeviceEntity createDemoDevice() {
        return createDemoDevice(getOrCreateDemoApplication());
    }

    private ApplicationEntity createDemoApplication(DemoProperties.DemoApplication demoApplication) {
        ApplicationEntity application = ApplicationEntity.builder()
                .name(demoApplication.getName())
                .appId(demoApplication.getAppId())
                .apiKey(demoApplication.getApiKey())
                .enabled(true)
                .build();

        ApplicationEntity savedDemoApplication = applicationRepository.save(application);

        log.info("Created demo application: {}", savedDemoApplication);

        return savedDemoApplication;
    }

    private DemoUser createDemoUser() {
        DemoUser.DemoUserBuilder demoUserBuilder = demoUserBuilderSupplier.get();
        List<String> authorities = Lists.newArrayList("ROLE_ADMIN", "ROLE_USER");

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

        return createDemoIssuer(DemoProperties.DemoIssuer.builder()
                .name(DEMO_ISSUER_NAME)
                .publicKeyBase64(encodeHexAsBase64(keys.getPublicKey()))
                .privateKeyBase64(encodeHexAsBase64(keys.getPrivateKey()))
                .build());
    }


    private IssuerEntity createDemoIssuer(DemoProperties.DemoIssuer demoIssuer) {
        IssuerEntity issuerEntity = IssuerEntity.builder()
                .name(demoIssuer.getName())
                .created(Date.from(Instant.EPOCH))
                .publicKeyBase64(encodeHexAsBase64(decodeBase64AsHex(demoIssuer.getPublicKeyBase64())))
                .privateKeyBase64(encodeHexAsBase64(decodeBase64AsHex(demoIssuer.getPrivateKeyBase64())))
                .build();

        IssuerEntity savedDemoIssuer = issuerRepository.save(issuerEntity);

        log.info("Created demo issuer: {}", savedDemoIssuer);

        return savedDemoIssuer;

    }

    private DemoUser.DemoUserBuilder createDemoUserBuilder() {
        String password = DEMO_USER_PASSWORD;
        return DemoUser.builder()
                .name(DEMO_USER_NAME)
                .password(password)
                .encryptedPassword(passwordEncoder.encode(password));
    }

    private ApplicationEntity createDemoApplication() {
        return createDemoApplication(DemoProperties.DemoApplication.builder()
                .name(DEMO_APP_NAME)
                .appId(SecureRandomUtils.generateRandomAppId().toLowerCase())
                .apiKey(DEMO_APP_API_KEY)
                .build());
    }

    private VehicleEntity createDemoVehicle(IssuerEntity issuerEntity) {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        String publicKeyBase64 = encodeHexAsBase64(keys.getPublicKey());

        final DemoProperties.DemoVehicle demoVehicle = DemoProperties.DemoVehicle.builder()
                .name(DEMO_VEHICLE_NAME)
                .serialNumber(SecureRandomUtils.generateRandomSerial().toLowerCase())
                .publicKeyBase64(publicKeyBase64)
                .build();

        return createDemoVehicle(issuerEntity, demoVehicle);
    }

    private VehicleEntity createDemoVehicle(IssuerEntity demoIssuer, DemoProperties.DemoVehicle demoVehicle) {
        VehicleEntity vehicleEntity = VehicleEntity.builder()
                .issuerId(demoIssuer.getId())
                .name(demoVehicle.getName())
                .serialNumber(demoVehicle.getSerialNumber())
                .publicKeyBase64(demoVehicle.getPublicKeyBase64())
                .build();

        final VehicleEntity savedVehicleEntity = vehicleRepository.save(vehicleEntity);

        log.info("Created demo vehicle: {}", savedVehicleEntity);

        return savedVehicleEntity;
    }

    private DeviceEntity createDemoDevice(ApplicationEntity applicationEntity) {
        DeviceWithKeys demoDeviceWithKeys = createDemoDeviceWithKeys(applicationEntity);

        return demoDeviceWithKeys.getDevice();
    }


}
