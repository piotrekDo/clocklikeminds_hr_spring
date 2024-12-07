package com.example.clocklike_portal.timeoff;

import com.example.clocklike_portal.timeoff.on_saturday.HolidayOnSaturdayByUserDto;
import com.example.clocklike_portal.timeoff.on_saturday.HolidayOnSaturdaySummaryDto;
import com.example.clocklike_portal.timeoff.on_saturday.SaturdayHolidayDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/pto")
@AllArgsConstructor
public class TimeOffController {

    private final TimeOffService timeOffService;

    @PostMapping("/request-new")
    TimeOffDto requestPto(@RequestBody @Valid NewPtoRequest dto) {
        return timeOffService.processNewRequest(dto);
    }

    @GetMapping("/byId")
    Page<TimeOffDto> findAllRequestsByAppliersId(@RequestParam Long id,
                                                 @RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size) {
        return timeOffService.getPtoRequestsByApplier(id, page, size);
    }

    @GetMapping("/unresolved-by-acceptor")
    List<TimeOffDto> findAllUnresolvedPtoRequestsByAcceptor(@RequestParam Long id) {
        return timeOffService.findAllUnresolvedPtoRequestsByAcceptor(id);
    }

    @GetMapping("/requests-by-acceptor")
    List<TimeOffDto> findAllRequestsByAcceptorId(@RequestParam long acceptorId) {
        return timeOffService.findAllRequestsByAcceptorId(acceptorId);
    }

    @PostMapping("/resolve-request")
    TimeOffDto resolveRequest(@RequestBody ResolvePtoRequest resolveRequestDto) {
        return timeOffService.resolveRequest(resolveRequestDto);
    }

    @GetMapping("/summary")
    PtoSummary getUserSummary(@RequestParam Long id) {
        return timeOffService.getUserPtoSummary(id);
    }

    @GetMapping("/requests-for-year")
    List<TimeOffDto> getRequestsForSelectedYear(@RequestParam Long userId, @RequestParam Integer year) {
        return timeOffService.getRequestsForUserForYear(year, userId);
    }

    @GetMapping("/requests-for-supervisor-calendar")
    List<TimeOffDto> getRequestsForSupervisorCalendar(@RequestParam Long acceptorId, @RequestParam String start, @RequestParam String end) {
        return timeOffService.getRequestsForSupervisorCalendar(acceptorId, start, end);
    }

    @PostMapping("/new-saturday-holiday")
    SaturdayHolidayDto addNewHolidayOnSaturday(@RequestBody SaturdayHolidayDto dto) {
        return timeOffService.registerNewHolidaySaturday(dto);
    }

    @GetMapping("/holidays-on-saturday-admin")
    HolidayOnSaturdaySummaryDto getHolidaysOnSaturdaySummaryForAdmin(@RequestParam(required = false) Integer year) {
        return timeOffService.getHolidaysOnSaturdaySummaryForAdmin(year);
    }

    /**
     *
     * @param supervisorId pass -1 for All users, regardless supervisor
     */
    @GetMapping("/holiday-on-saturday-by-users")
    List<HolidayOnSaturdayByUserDto> getHolidayBySaturdayByUsers(@RequestParam(required = true) Long holidayId,
                                                                 @RequestParam(required = true) Long supervisorId){
        return timeOffService.getHolidayOnSaturdaySummaryByUsers(holidayId, supervisorId);
    }

    @PostMapping("/withdraw")
    WithdrawResponse withdrawTimeOffRequest(@RequestParam Long requestId, @RequestParam String applierNotes) {
        return timeOffService.withdrawTimeOffRequest(requestId, applierNotes);
    }
}
