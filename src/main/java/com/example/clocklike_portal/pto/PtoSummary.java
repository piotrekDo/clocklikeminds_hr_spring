package com.example.clocklike_portal.pto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class PtoSummary {
    private int ptoDaysFromLastYear;
    private int ptoDaysCurrentYear;
    private int ptoDaysTaken;
    private int ptoDaysPending;
    private List<PtoDto> ptoRequests;
}
