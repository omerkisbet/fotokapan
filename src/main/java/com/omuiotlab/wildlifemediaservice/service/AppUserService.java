package com.omuiotlab.wildlifemediaservice.service;

import com.omuiotlab.wildlifemediaservice.dto.CustomerCreateRequest;
import com.omuiotlab.wildlifemediaservice.dto.CustomerResponse;
import com.omuiotlab.wildlifemediaservice.exception.DuplicateEmailException;
import com.omuiotlab.wildlifemediaservice.exception.UserNotFoundException;
import com.omuiotlab.wildlifemediaservice.model.AppUser;
import com.omuiotlab.wildlifemediaservice.model.UserRole;
import com.omuiotlab.wildlifemediaservice.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        AppUser customer = AppUser.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CUSTOMER)
                .active(true)
                .build();

        return CustomerResponse.from(appUserRepository.save(customer));
    }

    public List<CustomerResponse> listCustomers() {
        return appUserRepository.findByRoleOrderByFullNameAsc(UserRole.CUSTOMER)
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    public AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("oturum");
        }
        return getByEmail(authentication.getName());
    }

    public AppUser getByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return appUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(normalizedEmail));
    }

    public AppUser getCustomerById(String customerId) {
        AppUser user = appUserRepository.findById(customerId)
                .orElseThrow(() -> new UserNotFoundException(customerId));

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("Seçilen kullanıcı müşteri rolünde değil.");
        }
        return user;
    }

    public boolean isAdmin(AppUser user) {
        return user.getRole() == UserRole.ADMIN;
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("E-posta adresi boş olamaz.");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
