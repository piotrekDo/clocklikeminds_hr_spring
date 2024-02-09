package com.example.clocklike_portal.job_position;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class PositionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionHistoryId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "positionId")
    private PositionEntity position;
    private LocalDate startDate;

    public static PositionHistory createNewPositionHistory(PositionEntity position, LocalDate startDate) {
        return new PositionHistory(
                null,
                position,
                startDate
        );
    }

    ;
}
