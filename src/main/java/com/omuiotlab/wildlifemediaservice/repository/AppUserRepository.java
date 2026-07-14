package com.omuiotlab.wildlifemediaservice.repository;

import com.omuiotlab.wildlifemediaservice.model.AppUser;
import com.omuiotlab.wildlifemediaservice.model.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends MongoRepository<AppUser, String> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findByRoleOrderByFullNameAsc(UserRole role);
}
