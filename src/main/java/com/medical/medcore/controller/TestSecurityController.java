package com.medical.medcore.controller;

import com.medical.medcore.security.authorization.annotation.RequireSuperAdmin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-security")
public class TestSecurityController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "public ok";
    }

    @RequireSuperAdmin
    @GetMapping("/admin")
    public String adminEndpoint() {
        return "admin ok";
    }
}