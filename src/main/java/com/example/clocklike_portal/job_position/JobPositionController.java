package com.example.clocklike_portal.job_position;

import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/positions")
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
