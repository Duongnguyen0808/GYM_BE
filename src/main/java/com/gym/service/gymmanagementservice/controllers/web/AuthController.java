package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.services.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}