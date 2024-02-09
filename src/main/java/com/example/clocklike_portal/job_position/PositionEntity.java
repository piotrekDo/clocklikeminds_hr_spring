package com.example.clocklike_portal.job_position;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity(name = "job_positions")
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
public class PositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long positionId;
    private String positionKey;
    private String displayName;

    public PositionEntity(String positionKey, String displayName) {
        this.positionKey = positionKey;
        this.displayName = displayName;
    }
}
