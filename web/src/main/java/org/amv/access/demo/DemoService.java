package org.amv.access.demo;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.amv.access.Application;
import org.amv.access.model.User;
import org.amv.access.model.UserRepository;
import org.amv.access.model.Vehicle;
import org.amv.access.model.VehicleRepository;
import org.amv.highmobility.cryptotool.Cryptotool;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

import static java.util.Objects.requireNonNull;

@Slf4j
public class DemoService {

    private final Cryptotool cryptotool;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    private final Supplier<DemoUser.DemoUserBuilder> demoUserBuilderSupplier = Suppliers
            .memoize(this::createDemoUserBuilder);

    public DemoService(Cryptotool cryptotool,
                       PasswordEncoder passwordEncoder,
                       UserRepository userRepository,
                       VehicleRepository vehicleRepository) {
        this.cryptotool = cryptotool;
        this.passwordEncoder = requireNonNull(passwordEncoder);
        this.userRepository = requireNonNull(userRepository);
        this.vehicleRepository = vehicleRepository;
    }

    public DemoUser getOrCreateDemoUser() {
        final User user = userRepository.findByName("demo", Application.standardPageRequest)
                .getContent()
                .stream()
                .findFirst()
                .orElseGet(() -> createDemoUser().getOrigin());

        return demoUserBuilderSupplier.get()
                .origin(user)
                .build();
    }

    public void createDemoData() {
        boolean hasDemoData = vehicleRepository.findAll(new PageRequest(0, 1)).hasContent();
        if (hasDemoData) {
            return;
        }

        Cryptotool.Keys keys = cryptotool.generateKeys().block();

        Vehicle vehicle = Vehicle.builder()
                .id(10L)
                .serialNumber(RandomStringUtils.randomNumeric(18))
                .publicKey(keys.getPublicKey())
                .build();

        vehicleRepository.save(vehicle);
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
        final String username = "demo";
        final String password = "demo";
        return DemoUser.builder()
                .name(username)
                .password(password)
                .encryptedPassword(passwordEncoder.encode(password));
    }

}
