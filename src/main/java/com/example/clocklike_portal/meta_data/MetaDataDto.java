package com.example.clocklike_portal.meta_data;

import com.example.clocklike_portal.settings.Settings;
import lombok.Data;

import java.util.List;

import static com.example.clocklike_portal.app.Library.*;

@Data
public class MetaDataDto {
    private List<String> requestTypes;
    private List<OccasionalLeaveTypeDto> occasionalLeaveTypes;
    private List<Settings> settings;

    public MetaDataDto(List<OccasionalLeaveTypeDto> occasionalLeaveTypes, List<Settings> settings) {
        this.requestTypes = List.of(PTO_DISCRIMINATOR_VALUE, PTO_ON_DEMAND_DISCRIMINATOR_VALUE, CHILD_CARE_LEAVE_DISCRIMINATOR_VALUE, ON_SATURDAY_PTO_DISCRIMINATOR_VALUE, OCCASIONAL_LEAVE_DISCRIMINATOR_VALUE);
        this.occasionalLeaveTypes = occasionalLeaveTypes;
        this.settings = settings;
    }
}
