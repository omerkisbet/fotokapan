package com.omuiotlab.wildlifemediaservice.controller;

import com.omuiotlab.wildlifemediaservice.dto.CustomerCreateRequest;
import com.omuiotlab.wildlifemediaservice.dto.CustomerResponse;
import com.omuiotlab.wildlifemediaservice.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final AppUserService appUserService;

    public AdminCustomerController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return appUserService.listCustomers();
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerCreateRequest request
    ) {
        return ResponseEntity.status(201).body(appUserService.createCustomer(request));
    }
}
