package com.example.clocklike_portal.report;

import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.example.clocklike_portal.security.SecurityConfig.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/report")
@AllArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/generate-creative-work-report-template")
    ResponseEntity<byte[]> generateCreativeWorkReportTemplate(@RequestParam int monthIndex,
                                                              @RequestParam int year) {
        byte[] report = reportService.generateCreativeWorkReportTemplate(monthIndex, year);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("template.xlsx").build());
        return new ResponseEntity<>(report, headers, HttpStatus.OK);
    }
}
