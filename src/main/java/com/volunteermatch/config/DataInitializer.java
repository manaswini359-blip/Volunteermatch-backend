package com.volunteermatch.config;

import com.volunteermatch.entity.User;
import com.volunteermatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Only predefined demo seed accounts are managed here
    private static final Set<String> PREDEFINED_DEMO_ACCOUNTS = Set.of(
            "contact@abcfoundation.org",
            "info@greenearth.org",
            "hello@helpinghands.org",
            "ananya@example.com",
            "rahul@example.com",
            "sneha@example.com",
            "arjun@example.com",
            "priya@example.com"
    );

    @Override
    public void run(String... args) {
        log.info("Checking predefined demo accounts...");
        for (String email : PREDEFINED_DEMO_ACCOUNTS) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (!passwordEncoder.matches("password123", user.getPassword())) {
                    user.setPassword(passwordEncoder.encode("password123"));
                    userRepository.save(user);
                    log.info("Synchronized password for demo account: {}", email);
                }
            });
        }
        log.info("DataInitializer finished: Predefined demo accounts verified with 'password123'. All user-registered accounts remain completely untouched.");
    }
}
