package com.example.clocklike_portal.job_position;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@AllArgsConstructor
@Data
@ToString
public class NewJobPositionRequest {
    @NotBlank(groups = {AddPosition.class}, message = "Job position key cannot be blank")
    private String positionKey;
    @NotBlank(groups = {AddPosition.class}, message = "Job position name cannot be blank")
    private String displayName;
}

interface AddPosition {

}

interface UpdatePosition{

}