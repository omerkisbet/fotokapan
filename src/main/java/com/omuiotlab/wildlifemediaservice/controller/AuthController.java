package com.omuiotlab.wildlifemediaservice.controller;

import com.omuiotlab.wildlifemediaservice.dto.CsrfResponse;
import com.omuiotlab.wildlifemediaservice.dto.CurrentUserResponse;
import com.omuiotlab.wildlifemediaservice.service.AppUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(
                token.getHeaderName(),
                token.getParameterName(),
                token.getToken()
        );
    }

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        return CurrentUserResponse.from(appUserService.getCurrentUser(authentication));
    }
}
