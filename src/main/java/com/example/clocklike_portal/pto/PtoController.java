package com.example.clocklike_portal.pto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

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
}
