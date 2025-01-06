package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.appUser.AppUserBasicDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class TimeOffRequestsByEmployee {
    private final AppUserBasicDto employee;
    private final List<TimeOffDto> requestsByTimeFrame;
}
