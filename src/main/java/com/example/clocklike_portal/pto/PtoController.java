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
        return ptoService.requestPto(dto);
    }

    @GetMapping("/byId")
    Page<PtoDto> findAllRequestsByAppliersId(@RequestParam Long id,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return ptoService.getPtoRequests(id, page, size);
    }

    @GetMapping("/requests-to-accept")
    List<PtoDto> findAllRequestAcceptByAcceptId(@RequestParam long acceptorId) {
        return ptoService.findAllRequestsToAcceptByAcceptId(acceptorId);
    }

    @PostMapping("/resolve-request")
    PtoDto resolveRequest(@RequestBody ResolvePtoRequest resolveRequestDto) {
        return ptoService.resolveRequest(resolveRequestDto);
    }

    @GetMapping("/summary")
    PtoSummary getUserSummary(@RequestParam Long id) {
        return ptoService.getUserPtoSummary(id);
    }
}
