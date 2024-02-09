package com.example.clocklike_portal.appUser;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @PostMapping("/finish-register")
    AppUserDto finishUserRegister(@RequestBody RegistrationFinishRequest request) {
        return appUserService.finishRegistration(request);
    }

    @GetMapping("/all-users")
    Page<AppUserBasicDto> getAllAppUsersPage(@RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return appUserService.findAllUsers(page, size);
    }

    @GetMapping("/{id}")
    AppUserDto getAppUserById(@PathVariable Long id){
        return appUserService.getAppUserById(id);
    }
}
