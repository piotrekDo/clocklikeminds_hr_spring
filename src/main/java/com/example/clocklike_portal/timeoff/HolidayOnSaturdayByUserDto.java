package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.appUser.AppUserBasicDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class HolidayOnSaturdayByUserDto {
    private SaturdayHolidayDto holiday;
    private AppUserBasicDto employee;
    private PtoDto pto;
}
