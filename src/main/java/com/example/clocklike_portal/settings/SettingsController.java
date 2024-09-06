package com.example.clocklike_portal.settings;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@AllArgsConstructor
public class SettingsController {

    private final SettingsService service;

    @GetMapping("/get-all")
    List<Settings> getAllSettings() {
        return service.getSettings();
    }

    @GetMapping("/switch-mailing")
    boolean switchMailing() {
        return service.switchMailingEnabled();
    }

}
