package com.omuiotlab.wildlifemediaservice.config;

import com.omuiotlab.wildlifemediaservice.model.AppUser;
import com.omuiotlab.wildlifemediaservice.model.UserRole;
import com.omuiotlab.wildlifemediaservice.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Order(1)
public class UserDataInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-email}")
    private String adminEmail;

    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Value("${app.bootstrap.customer-email}")
    private String customerEmail;

    @Value("${app.bootstrap.customer-password}")
    private String customerPassword;

    public UserDataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing("Sistem Yöneticisi", adminEmail, adminPassword, UserRole.ADMIN);
        createIfMissing("Demo Müşteri", customerEmail, customerPassword, UserRole.CUSTOMER);
    }

    private void createIfMissing(
            String fullName,
            String rawEmail,
            String rawPassword,
            UserRole role
    ) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        AppUser user = AppUser.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(true)
                .build();

        appUserRepository.save(user);
    }
}
