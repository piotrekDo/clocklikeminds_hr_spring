package com.example.clocklike_portal.timeoff.on_saturday;

import com.example.clocklike_portal.appUser.AppUserBasicDto;
import com.example.clocklike_portal.timeoff.TimeOffDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class HolidayOnSaturdayByUserDto {
    private SaturdayHolidayDto holiday;
    private AppUserBasicDto employee;
    private TimeOffDto pto;

    public static HolidayOnSaturdayByUserDto fromEntity(HolidayOnSaturdayUserEntity entity, TimeOffDto timeOffDto) {
        return new HolidayOnSaturdayByUserDto(
                SaturdayHolidayDto.fromEntity(entity.getHoliday()),
                AppUserBasicDto.appUserEntityToBasicDto(entity.getUser()),
                timeOffDto
        );
    }
}
