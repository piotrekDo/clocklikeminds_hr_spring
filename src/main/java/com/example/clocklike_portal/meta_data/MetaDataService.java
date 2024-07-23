package com.example.clocklike_portal.meta_data;

import com.example.clocklike_portal.timeoff.OccasionalLeaveTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MetaDataService {
    private final OccasionalLeaveTypeRepository occasionalLeaveTypeRepository;

    MetaDataDto getMetaData() {
        List<OccasionalLeaveTypeDto> occasionalLeaveTypes = occasionalLeaveTypeRepository.findAll().stream()
                .map(type -> new OccasionalLeaveTypeDto(type.getId(), type.getOccasionalType(), type.getDescriptionPolish(), type.getDays()))
                .toList();
        return new MetaDataDto(occasionalLeaveTypes);
    }
}
