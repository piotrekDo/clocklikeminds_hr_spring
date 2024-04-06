package com.example.clocklike_portal.appUser;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@AllArgsConstructor
@Data
@ToString
public class FinishRegistrationRequest {
    private Long appUserId;
    private String positionKey;
    private String hireStart;
    private String hireEnd;
    private Integer ptoDaysTotal;

    private Boolean isStillHired;
}
