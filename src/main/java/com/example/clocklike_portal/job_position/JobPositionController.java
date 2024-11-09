package com.example.clocklike_portal.job_position;

import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/positions")
@AllArgsConstructor
public class JobPositionController {

    private final JobPositionService positionService;

    @GetMapping("/all")
    List<PositionEntity> getAll() {
        return positionService.getAll();
    }

    @PostMapping()
    PositionEntity addNewPosition(@RequestBody @Validated(value = AddPosition.class) NewJobPositionRequest request) {
        return positionService.addNew(request);
    }
}
