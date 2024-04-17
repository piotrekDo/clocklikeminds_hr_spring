package com.example.clocklike_portal.appUser;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UpdateUserPermissionRequest {
    private Long appUserId;
    private Boolean hasAdminPermission;
    private Boolean hasSupervisorRole;
    private Boolean isActive;
}
