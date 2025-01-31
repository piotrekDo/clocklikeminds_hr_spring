package com.example.clocklike_portal.settings;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/settings")
@AllArgsConstructor
public class SettingsController {

    private final SettingsService service;

    @GetMapping("/get-all")
    List<Settings> getAllSettings() {
        return service.getSettings();
    }

    @GetMapping("/switch-mailing-local")
    boolean switchMailingLocal() {
        return service.switchMailingLocalEnabled();
    }

    @GetMapping("/switch-mailing-hr")
    boolean switchMailingHr() {
        return service.switchMailingHrEnabled();
    }

}
