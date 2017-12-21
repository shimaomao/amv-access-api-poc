package org.amv.access.demo;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.auth.ApplicationAuthenticationImpl;
import org.amv.access.auth.IssuerNonceAuthentication;
import org.amv.access.auth.IssuerNonceAuthenticationImpl;
import org.amv.access.auth.NonceAuthentication;
import org.amv.access.certificate.AccessCertificateService;
import org.amv.access.certificate.AccessCertificateService.CreateAccessCertificateContext;
import org.amv.access.certificate.DeviceCertificateService;
import org.amv.access.certificate.DeviceCertificateService.CreateDeviceCertificateContext;
import org.amv.access.certificate.SignedAccessCertificateResource;
import org.amv.access.core.Device;
import org.amv.access.core.DeviceCertificate;
import org.amv.access.core.Key;
import org.amv.access.core.impl.KeyImpl;
import org.amv.access.model.*;
import org.amv.access.spi.highmobility.NonceAuthenticationService;
import org.amv.access.spi.highmobility.NonceAuthenticationServiceImpl;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.amv.highmobility.cryptotool.CryptotoolUtils.SecureRandomUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private static final String DEMO_APP_ID = "0000123456789abcdef00000";
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
    private final DeviceCertificateService deviceCertificateService;
    private final AccessCertificateService accessCertificateService;

    private final Supplier<DemoUser.DemoUserBuilder> demoUserBuilderSupplier = Suppliers
            .memoize(this::createDemoUserBuilder);
    private final NonceAuthenticationService nonceAuthService;

    public DemoServiceImpl(Cryptotool cryptotool,
                           PasswordEncoder passwordEncoder,
                           IssuerRepository issuerRepository,
                           ApplicationRepository applicationRepository,
                           UserRepository userRepository,
                           VehicleRepository vehicleRepository,
                           DeviceRepository deviceRepository,
                           DeviceCertificateService deviceCertificateService,
                           AccessCertificateService accessCertificateService) {
        this.cryptotool = requireNonNull(cryptotool);
        this.passwordEncoder = requireNonNull(passwordEncoder);
        this.issuerRepository = requireNonNull(issuerRepository);
        this.applicationRepository = requireNonNull(applicationRepository);
        this.userRepository = requireNonNull(userRepository);
        this.vehicleRepository = requireNonNull(vehicleRepository);
        this.deviceRepository = requireNonNull(deviceRepository);
        this.deviceCertificateService = requireNonNull(deviceCertificateService);
        this.accessCertificateService = requireNonNull(accessCertificateService);

        this.nonceAuthService = new NonceAuthenticationServiceImpl(cryptotool);
    }

    private void createDefaultDemoData() {
        this.getOrCreateDemoUser();
        this.getOrCreateDemoApplication();
        this.getOrCreateDemoDevice();
        this.getOrCreateDemoVehicle();
    }

    @Override
    @Transactional
    public void createDemoDataFromProperties(DemoProperties demoProperties) {
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

        createDefaultDemoData();

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

        Key publicKey = KeyImpl.fromHex(keys.getPublicKey());
        Key privateKey = KeyImpl.fromHex(keys.getPrivateKey());

        ApplicationAuthenticationImpl appAuth = ApplicationAuthenticationImpl.builder()
                .application(applicationEntity)
                .build();

        CreateDeviceCertificateContext deviceCertificateRequest = CreateDeviceCertificateContext.builder()
                .appId(applicationEntity.getAppId().toHex())
                .deviceName("DEMO")
                .devicePublicKeyBase64(publicKey.toBase64())
                .build();

        DeviceCertificate deviceCertificate = Optional.ofNullable(deviceCertificateService)
                .map(service -> service.createDeviceCertificate(appAuth, deviceCertificateRequest))
                .map(Mono::block)
                .orElseThrow(IllegalStateException::new);

        Device demoDevice = deviceCertificate.getDevice();

        DeviceEntity demoDeviceEntity = deviceRepository.findBySerialNumber(demoDevice.getSerialNumber().toHex())
                .orElseThrow(IllegalStateException::new);

        log.info("Created device certificate for device {}: {}", demoDevice.getSerialNumber(), deviceCertificate);

        return DeviceWithKeys.builder()
                .device(demoDeviceEntity)
                .publicKey(publicKey)
                .privateKey(privateKey)
                .build();
    }

    @Override
    public Mono<SignedAccessCertificateResource> createDemoAccessCertificateIfNecessary(DeviceCertificate deviceCertificate) {
        ApplicationEntity demoApplication = this.getOrCreateDemoApplication();

        boolean isDeviceCertificateForDemoApplication = demoApplication.getAppId().toHex()
                .equals(deviceCertificate.getApplication().getAppId().toHex());

        if (!isDeviceCertificateForDemoApplication) {
            log.debug("Skip creating demo access certificate for device {}: Device Certificate is not issued for demo application",
                    deviceCertificate.getDevice().getSerialNumber());

            return Mono.empty();
        } else {
            IssuerWithKeys issuerWithKeys = this.getOrCreateDemoIssuer();
            VehicleEntity demoVehicle = this.getOrCreateDemoVehicle();
            Device device = deviceCertificate.getDevice();

            //String issuerPrivateKeyBase64 = MoreBase64.encodeHexAsBase64(issuerWithKeys.getPrivateKey());

            log.info("Creating demo access certificate for device {} and vehicle {}",
                    device.getSerialNumber(),
                    demoVehicle.getSerialNumber());

            IssuerNonceAuthentication issuerNonceAuthentication = createDemoIssuerNonceAuthentication();

            CreateAccessCertificateContext createAccessCertificateContext = CreateAccessCertificateContext.builder()
                    .appId(demoApplication.getAppId().toHex())
                    .deviceSerialNumber(device.getSerialNumber().toHex())
                    .vehicleSerialNumber(demoVehicle.getSerialNumber().toHex())
                    .validityStart(LocalDateTime.now().minusDays(2).toInstant(ZoneOffset.UTC))
                    .validityEnd(LocalDateTime.now().plusYears(2).toInstant(ZoneOffset.UTC))
                    .build();

            Optional<SignedAccessCertificateResource> signedAccessCertificateResource = Optional.ofNullable(accessCertificateService
                    .createAccessCertificate(issuerNonceAuthentication, createAccessCertificateContext)
                    .then(resource -> accessCertificateService.signAccessCertificate(resource, issuerWithKeys.getPrivateKey())))
                    .map(Mono::block);

            if (!signedAccessCertificateResource.isPresent()) {
                log.warn("Could not create demo access certificate for device {}", device.getSerialNumber());
            } else {
                log.info("Successfully created demo access certificate for device {}", device.getSerialNumber());
            }

            return Mono.justOrEmpty(signedAccessCertificateResource);
        }
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
    public IssuerNonceAuthentication createNonceAuthentication(IssuerWithKeys issuerWithKeys) {
        NonceAuthentication nonceAuthentication = nonceAuthService.createNonceAuthentication(issuerWithKeys.getPrivateKey());
        IssuerNonceAuthentication issuerNonceAuthentication = IssuerNonceAuthenticationImpl.builder()
                .issuerUuid(issuerWithKeys.getIssuer().getUuid())
                .nonceAuthentication(nonceAuthentication)
                .build();

        return issuerNonceAuthentication;
    }

    @Override
    public IssuerWithKeys getOrCreateDemoIssuer() {
        IssuerEntity demoIssuer = issuerRepository.findByName(DEMO_ISSUER_NAME, new PageRequest(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(() -> this.createDemoIssuerWithKeys().getIssuer());

        String privateKeyBase64 = demoIssuer.getPrivateKeyBase64()
                .orElseThrow(IllegalArgumentException::new);

        return IssuerWithKeys.builder()
                .issuer(demoIssuer)
                .publicKey(KeyImpl.fromBase64(demoIssuer.getPublicKeyBase64()))
                .privateKey(KeyImpl.fromBase64(privateKeyBase64))
                .build();
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

    private IssuerWithKeys createDemoIssuerWithKeys() {
        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        IssuerEntity demoIssuer = createDemoIssuer(DemoProperties.DemoIssuer.builder()
                .name(DEMO_ISSUER_NAME)
                .publicKeyBase64(encodeHexAsBase64(keys.getPublicKey()))
                .privateKeyBase64(encodeHexAsBase64(keys.getPrivateKey()))
                .build());

        log.info("created Issuer with keys: {}\n{}\n{}", demoIssuer,
                keys.getPublicKey(),
                keys.getPrivateKey());

        return IssuerWithKeys.builder()
                .issuer(demoIssuer)
                .publicKey(demoIssuer.getPublicKey())
                .privateKey(demoIssuer.getPrivateKey()
                        .orElseThrow(IllegalStateException::new))
                .build();
    }

    private VehicleEntity createDemoVehicle() {
        return createDemoVehicle(getOrCreateDemoIssuer().getIssuer());
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

        UserEntity demoUser = UserEntity.builder()
                .name(demoUserBuilder.name())
                .password(demoUserBuilder.encryptedPassword())
                .salt("")
                .build();

        userRepository.save(demoUser);
        log.info("Created demo user: {}", demoUser);

        return demoUserBuilder
                .origin(demoUser)
                .build();
    }

    private IssuerEntity createDemoIssuer(DemoProperties.DemoIssuer demoIssuer) {
        IssuerEntity issuerEntity = IssuerEntity.builder()
                .name(demoIssuer.getName())
                .uuid(UUID.randomUUID().toString())
                .createdAt(Date.from(Instant.EPOCH.plusSeconds(TimeUnit.DAYS.toSeconds(1))))
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
                .appId(DEMO_APP_ID)
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
