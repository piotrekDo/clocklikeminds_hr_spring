package com.example.clocklike_portal.dashboard;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/dashboard")
@AllArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/supervisor")
    SupervisorDashboardDto getSupervisorDashboard(@RequestParam String calendarStart,
                                                  @RequestParam String calendarEnd) {
        return dashboardService.getSupervisorDashboardData(calendarStart, calendarEnd);
    }
}
