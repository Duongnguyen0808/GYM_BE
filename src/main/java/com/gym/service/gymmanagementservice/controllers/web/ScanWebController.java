package com.gym.service.gymmanagementservice.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ScanWebController {

    @GetMapping("/scan")
    public String scanPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("pageTitle", "Quét QR Code");
        model.addAttribute("contentView", "scan");
        model.addAttribute("activePage", "scan");
        return "fragments/layout";
    }
}