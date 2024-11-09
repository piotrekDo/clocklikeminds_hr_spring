package com.example.clocklike_portal.appUser;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/users")
@AllArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @PostMapping("/finish-register")
    AppUserDto finishUserRegister(@RequestBody FinishRegistrationRequest request) {
        return appUserService.finishRegistration(request);
    }

    @GetMapping("/all-users")
    Page<AppUserBasicDto> getAllAppUsersPage(@RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return appUserService.findAllUsers(page, size);
    }

    @GetMapping("/employees-by-supervisor")
    List<EmployeeInfo> getEmployeesBySupervisor(@RequestParam Long supervisorId) {
        return appUserService.getEmployeesBySupervisorId(supervisorId);
    }

    @GetMapping("/{id}")
    AppUserDto getAppUserById(@PathVariable Long id) {
        return appUserService.getAppUserById(id);
    }

    @GetMapping("/supervisors")
    List<AppUserBasicDto> getAllSupervisors() {
        return appUserService.getAllSupervisors();
    }

    @PostMapping("/update-hire-data")
    AppUserDto updateHireData(@RequestBody UpdateHireDataRequest request) {
        return appUserService.updateHireData(request);
    }

    @PostMapping("/update-holiday-data")
    AppUserDto updateHolidayData(@RequestBody UpdateEmployeeHolidayDataRequest request) {
        return appUserService.updateHolidayData(request);
    }

    @PostMapping("/{id}/update-position-history")
    AppUserDto updatePositionHistory(@RequestBody List<UpdatePositionHistoryRequest> requests, @PathVariable Long id) {
        return appUserService.updatePositionHistoryData(requests, id);
    }

    @PostMapping("/update-permission")
    AppUserDto updateUserPermission(@RequestBody UpdateUserPermissionRequest request) {
        return appUserService.updateUserPermission(request);
    }
}
