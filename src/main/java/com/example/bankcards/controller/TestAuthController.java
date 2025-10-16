package com.example.bankcards.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestAuthController {

    @GetMapping("/public")
    public String publicAccess() {
        return "Public Content - No authentication required";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String userAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return "User Content - Authenticated as: " + auth.getName();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return "Admin Board - Authenticated as: " + auth.getName();
    }
}