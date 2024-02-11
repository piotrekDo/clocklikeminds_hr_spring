package com.example.clocklike_portal.pto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@AllArgsConstructor
@Data
public class NewPtoRequest {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String ptoStart;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private String ptoEnd;
    private Long applierId;
    private Long acceptorId;
}
