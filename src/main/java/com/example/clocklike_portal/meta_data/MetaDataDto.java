package com.example.clocklike_portal.meta_data;

import lombok.Data;

import java.util.List;

import static com.example.clocklike_portal.app.Library.*;

@Data
public class MetaDataDto {
    private List<String> requestTypes;
    private List<OccasionalLeaveTypeDto> occasionalLeaveTypes;

    public MetaDataDto(List<OccasionalLeaveTypeDto> occasionalLeaveTypes) {
        this.requestTypes = List.of(PTO_DISCRIMINATOR_VALUE, PTO_ON_DEMAND_DISCRIMINATOR_VALUE, CHILD_CARE_LEAVE_DISCRIMINATOR_VALUE, ON_SATURDAY_PTO_DISCRIMINATOR_VALUE, OCCASIONAL_LEAVE_DISCRIMINATOR_VALUE);
        this.occasionalLeaveTypes = occasionalLeaveTypes;
    }
}
