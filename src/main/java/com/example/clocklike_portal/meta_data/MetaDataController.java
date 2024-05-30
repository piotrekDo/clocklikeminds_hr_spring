package com.example.clocklike_portal.meta_data;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
@AllArgsConstructor
public class MetaDataController {

    private final MetaDataService service;

    @GetMapping
    MetaDataDto getMetaData() {
        return service.getMetaData();
    }
}
