package org.amv.access.demo;

import com.google.common.collect.Lists;
import org.amv.access.model.UserEntity;
import org.amv.access.model.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static java.util.Objects.requireNonNull;

public class FakeUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public FakeUserDetailsService(UserRepository userRepository) {
        this.userRepository = requireNonNull(userRepository);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        final Page<UserEntity> byName = userRepository.findByName(username, new PageRequest(0, 1));

        return byName.getContent().stream().findFirst()
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
    }

    private UserDetails toUserDetails(UserEntity user) {

        return User.withUsername(user.getName())
                .password(user.getPassword())
                .authorities(Lists.newArrayList())
                .disabled(!user.isEnabled())
                .build();
    }
}
