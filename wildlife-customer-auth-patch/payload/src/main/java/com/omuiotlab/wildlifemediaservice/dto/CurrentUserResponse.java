package com.omuiotlab.wildlifemediaservice.dto;

import com.omuiotlab.wildlifemediaservice.model.AppUser;
import com.omuiotlab.wildlifemediaservice.model.UserRole;

public record CurrentUserResponse(
        String id,
        String fullName,
        String email,
        UserRole role
) {
    public static CurrentUserResponse from(AppUser user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
