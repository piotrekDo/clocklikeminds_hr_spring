package com.example.clocklike_portal.timeoff.on_saturday;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class SaturdayHolidayDto {
    private long id;
    private String date;
    private String note;
    private String usedDate;

    public static SaturdayHolidayDto fromEntity(HolidayOnSaturdayEntity entity) {
        return new SaturdayHolidayDto(
                entity.getId(),
                entity.getDate().toString(),
                entity.getNote(),
                null
        );
    }
}
