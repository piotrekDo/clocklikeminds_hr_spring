package com.example.clocklike_portal.meta_data;

import com.example.clocklike_portal.settings.Settings;
import com.example.clocklike_portal.settings.SettingsRepository;
import com.example.clocklike_portal.timeoff.occasional.OccasionalLeaveTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MetaDataService {
    private final OccasionalLeaveTypeRepository occasionalLeaveTypeRepository;
    private final SettingsRepository settingsRepository;

    MetaDataDto getMetaData() {
        List<OccasionalLeaveTypeDto> occasionalLeaveTypes = occasionalLeaveTypeRepository.findAll().stream()
                .map(type -> new OccasionalLeaveTypeDto(type.getId(), type.getOccasionalType(), type.getDescriptionPolish(), type.getDays()))
                .toList();
        List<Settings> settings = settingsRepository.findAll();
        return new MetaDataDto(occasionalLeaveTypes, settings);
    }
}
