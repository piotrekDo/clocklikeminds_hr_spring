package com.example.clocklike_portal.settings;

import com.example.clocklike_portal.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SettingsService {
    public static final String MAILING_ENABLED = "mailingEnabled";
    private final EmailService emailService;
    private final SettingsRepository settingsRepository;

    List<Settings> getSettings() {
        return settingsRepository.findAll();
    }

    boolean switchMailingEnabled() {
        Settings mailingEnabled = settingsRepository.findBySettingName(MAILING_ENABLED)
                .orElseThrow(() -> new NoSuchElementException("mailingEnabled setting was not found"));
        boolean isEnabled = Boolean.parseBoolean(mailingEnabled.getSettingValue());
        boolean switchedValue = !isEnabled;
        mailingEnabled.setSettingValue(Boolean.toString(switchedValue));
        settingsRepository.save(mailingEnabled);
        emailService.setEnabled(switchedValue);
        return switchedValue;
    }
}
