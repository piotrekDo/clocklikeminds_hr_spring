package com.example.clocklike_portal.dashboard;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.appUser.AppUserRepository;
import com.example.clocklike_portal.appUser.UserDetailsAdapter;
import com.example.clocklike_portal.appUser.UserRole;
import com.example.clocklike_portal.timeoff.PtoEntity;
import com.example.clocklike_portal.timeoff.PtoRepository;
import com.example.clocklike_portal.timeoff.PtoTransformer;
import com.example.clocklike_portal.timeoff.TimeOffDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

import static com.example.clocklike_portal.security.SecurityConfig.ADMIN_AUTHORITY;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PtoRepository ptoRepository;
    private final AppUserRepository appUserRepository;
    private final PtoTransformer ptoTransformer;

    private static UserDetailsAdapter getUserDetails() {
        SecurityContext context = SecurityContextHolder.getContext();
        return (UserDetailsAdapter) context.getAuthentication().getPrincipal();
    }

    private boolean isRequesterAdmin() {
        return getUserDetails().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(ADMIN_AUTHORITY));
    }

    /**
     * method returning data for both supervisor and admin dashboard. Returns -1 for newEmployeesCount and inactiveEmployees
     * if user is not admin.
     */
    SupervisorDashboardDto getSupervisorDashboardData(String calendarStart, String calendarEnd) {
        long requesterId = getUserDetails().getUserId();
        AppUserEntity appUserEntity = appUserRepository.findById(requesterId)
                .orElseThrow(() -> new NoSuchElementException("No user found with id " + requesterId));
        LocalDate startDate = LocalDate.parse(calendarStart, dateFormatter);
        LocalDate endDate = LocalDate.parse(calendarEnd, dateFormatter);

        List<PtoEntity> unresolved = ptoRepository.findUnresolvedOrWithdrawnRequestsByAcceptorId(requesterId);
        int unresolvedCount = (int) unresolved.stream().filter(r -> r.getDecisionDateTime() == null && !r.isWasMarkedToWithdraw()).count();
        int markedToWithdraw = (int) unresolved.stream().filter(PtoEntity::isWasMarkedToWithdraw).count();
        int newEmployeesCount = -1;
        int inactiveEmployees = -1;

        List<TimeOffDto> requestForCalendar = isRequesterAdmin() ? ptoRepository.findRequestsInTimeFrame(startDate, endDate).stream()
                .map(ptoTransformer::ptoEntityToDto)
                .toList() :
                ptoRepository.findRequestsByAcceptorAndTimeFrame(requesterId, startDate, endDate).stream()
                        .map(ptoTransformer::ptoEntityToDto)
                        .toList();

        boolean isAdmin = appUserEntity.getUserRoles().stream()
                .map(UserRole::getRoleName)
                .anyMatch(ADMIN_AUTHORITY::equals);
        if (isAdmin) {
            List<AppUserEntity> employees = appUserRepository.findAllByIsRegistrationFinishedFalseOrIsActiveFalse();
            newEmployeesCount = (int) employees.stream().filter(e -> !e.isRegistrationFinished()).count();
            inactiveEmployees = (int) employees.stream().filter(e -> e.isRegistrationFinished() && !e.isActive()).count();
        }

        return new SupervisorDashboardDto(
                requestForCalendar,
                unresolvedCount,
                markedToWithdraw,
                newEmployeesCount,
                inactiveEmployees
        );
    }
}
