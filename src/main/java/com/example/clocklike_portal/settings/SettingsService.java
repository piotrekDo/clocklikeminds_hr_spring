package com.example.clocklike_portal.settings;

import com.example.clocklike_portal.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SettingsService {
    public static final String MAILING_LOCAL_ENABLED = "mailingLocalEnabled";
    public static final String MAILING_HR_ENABLED = "mailingHrEnabled";
    private final EmailService emailService;
    private final SettingsRepository settingsRepository;

    List<Settings> getSettings() {
        return settingsRepository.findAll();
    }

    boolean switchMailingLocalEnabled() {
        Settings mailingEnabled = settingsRepository.findBySettingName(MAILING_LOCAL_ENABLED)
                .orElseThrow(() -> new NoSuchElementException("Setting not found"));
        boolean isEnabled = Boolean.parseBoolean(mailingEnabled.getSettingValue());
        boolean switchedValue = !isEnabled;
        mailingEnabled.setSettingValue(Boolean.toString(switchedValue));
        settingsRepository.save(mailingEnabled);
        emailService.setMailingLocalEnabled(switchedValue);
        if (!switchedValue) {
            Settings mailingHrEnabled = settingsRepository.findBySettingName(MAILING_HR_ENABLED)
                    .orElseThrow(() -> new NoSuchElementException("Setting not found"));
            mailingHrEnabled.setSettingValue(Boolean.toString(false));
            settingsRepository.save(mailingHrEnabled);
            emailService.setMailingHrEnabled(false);
        }
        return switchedValue;
    }

    boolean switchMailingHrEnabled() {
        Settings mailingEnabled = settingsRepository.findBySettingName(MAILING_HR_ENABLED)
                .orElseThrow(() -> new NoSuchElementException("Setting not found"));
        boolean isEnabled = Boolean.parseBoolean(mailingEnabled.getSettingValue());
        boolean switchedValue = !isEnabled;
        mailingEnabled.setSettingValue(Boolean.toString(switchedValue));
        settingsRepository.save(mailingEnabled);
        emailService.setMailingHrEnabled(switchedValue);
        if (switchedValue) {
            Settings mailingLocalEnabled = settingsRepository.findBySettingName(MAILING_LOCAL_ENABLED)
                    .orElseThrow(() -> new NoSuchElementException("Setting not found"));
            mailingLocalEnabled.setSettingValue(Boolean.toString(true));
            settingsRepository.save(mailingLocalEnabled);
            emailService.setMailingLocalEnabled(true);
        }
        return switchedValue;
    }
}
