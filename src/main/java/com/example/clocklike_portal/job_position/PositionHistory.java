package com.example.clocklike_portal.job_position;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class PositionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionHistoryId;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionHistory that = (PositionHistory) o;
        return positionHistoryId.equals(that.positionHistoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positionHistoryId);
    }
}
