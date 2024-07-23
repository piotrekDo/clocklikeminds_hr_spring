package com.example.clocklike_portal.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ErrorEntity<T> {
    private int code;
    private String message;
    private T details;

}