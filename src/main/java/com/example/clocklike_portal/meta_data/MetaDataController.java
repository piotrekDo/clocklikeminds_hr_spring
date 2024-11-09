package com.example.clocklike_portal.meta_data;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/meta")
@AllArgsConstructor
public class MetaDataController {

    private final MetaDataService service;

    @GetMapping
    MetaDataDto getMetaData() {
        return service.getMetaData();
    }
}
