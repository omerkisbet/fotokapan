package com.omuiotlab.wildlifemediaservice.dto;

import com.omuiotlab.wildlifemediaservice.model.AppUser;

import java.time.Instant;

public record CustomerResponse(
        String id,
        String fullName,
        String email,
        boolean active,
        Instant createdAt
) {
    public static CustomerResponse from(AppUser user) {
        return new CustomerResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
