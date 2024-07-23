package com.example.clocklike_portal.meta_data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OccasionalLeaveTypeDto {
    private int id;
    private String occasionalType;
    private String descriptionPolish;
    private int days;
}
