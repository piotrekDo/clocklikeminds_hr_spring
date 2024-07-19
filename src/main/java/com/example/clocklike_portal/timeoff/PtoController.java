package com.example.clocklike_portal.timeoff;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pto")
@AllArgsConstructor
public class PtoController {

    private final TimeOffService timeOffService;

    @PostMapping("/request-new")
    PtoDto requestPto(@RequestBody @Valid NewPtoRequest dto) {
        return timeOffService.processNewRequest(dto);
    }

    @GetMapping("/byId")
    Page<PtoDto> findAllRequestsByAppliersId(@RequestParam Long id,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return timeOffService.getPtoRequests(id, page, size);
    }

    @GetMapping("/unresolved-by-acceptor")
    List<PtoDto> findAllUnresolvedPtoRequestsByAcceptor(@RequestParam Long id) {
        return timeOffService.findAllUnresolvedPtoRequestsByAcceptor(id);
    }

    @GetMapping("/requests-by-acceptor")
    List<PtoDto> findAllRequestsByAcceptorId(@RequestParam long acceptorId) {
        return timeOffService.findAllRequestsByAcceptorId(acceptorId);
    }

    @PostMapping("/resolve-request")
    PtoDto resolveRequest(@RequestBody ResolvePtoRequest resolveRequestDto) {
        return timeOffService.resolveRequest(resolveRequestDto);
    }

    @GetMapping("/summary")
    PtoSummary getUserSummary(@RequestParam Long id) {
        return timeOffService.getUserPtoSummary(id);
    }

    @GetMapping("/requests-for-year")
    List<PtoDto> getRequestsForSelectedYear(@RequestParam Long userId, @RequestParam Integer year) {
        return timeOffService.getRequestsForUserForYear(year, userId);
    }

    @GetMapping("/requests-for-supervisor-calendar")
    List<PtoDto> getRequestsForSupervisorCalendar(@RequestParam Long acceptorId, @RequestParam String start, @RequestParam String end) {
        return timeOffService.getRequestsForSupervisorCalendar(acceptorId, start, end);
    }

    @PostMapping("/new-saturday-holiday")
    SaturdayHolidayDto addNewHolidayOnSaturday(@RequestBody SaturdayHolidayDto dto) {
        return timeOffService.registerNewHolidaySaturday(dto);
    }

    @GetMapping("/holidays-on-saturday-admin")
    HolidayOnSaturdaySummaryDto getHolidaysOnSaturdaySummaryForAdmin() {
        return timeOffService.getHolidaysOnSaturdaySummaryForAdmin();
    }
}
