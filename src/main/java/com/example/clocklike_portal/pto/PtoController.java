package com.example.clocklike_portal.pto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pto")
@AllArgsConstructor
public class PtoController {

    private final PtoService ptoService;

    @PostMapping("/request-new")
    PtoDto requestPto(@RequestBody @Valid NewPtoRequest dto) {
        return ptoService.processNewRequest(dto);
    }

    @GetMapping("/byId")
    Page<PtoDto> findAllRequestsByAppliersId(@RequestParam Long id,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return ptoService.getPtoRequests(id, page, size);
    }

    @GetMapping("/unresolved-by-acceptor")
    List<PtoDto> findAllUnresolvedPtoRequestsByAcceptor(@RequestParam Long id) {
        return ptoService.findAllUnresolvedPtoRequestsByAcceptor(id);
    }

    @GetMapping("/requests-by-acceptor")
    List<PtoDto> findAllRequestsByAcceptorId(@RequestParam long acceptorId) {
        return ptoService.findAllRequestsByAcceptorId(acceptorId);
    }

    @PostMapping("/resolve-request")
    PtoDto resolveRequest(@RequestBody ResolvePtoRequest resolveRequestDto) {
        return ptoService.resolveRequest(resolveRequestDto);
    }

    @GetMapping("/summary")
    PtoSummary getUserSummary(@RequestParam Long id) {
        return ptoService.getUserPtoSummary(id);
    }

    @GetMapping("/requests-for-year")
    List<PtoDto> getRequestsForSelectedYear(@RequestParam Long userId, @RequestParam Integer year) {
        return ptoService.getRequestsForUserForYear(year, userId);
    }

    @GetMapping("/requests-for-supervisor-calendar")
    List<PtoDto> getRequestsForSupervisorCalendar(@RequestParam Long acceptorId, @RequestParam String start, @RequestParam String end) {
        return ptoService.getRequestsForSupervisorCalendar(acceptorId, start, end);
    }
}
