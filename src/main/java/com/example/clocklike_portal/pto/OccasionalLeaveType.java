package com.example.clocklike_portal.pto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class OccasionalLeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String occasionalType;
    private String descriptionPolish;
    private int days;


    public OccasionalLeaveType(String occasionalType, String descriptionPolish, int days) {
        this.occasionalType = occasionalType;
        this.descriptionPolish = descriptionPolish;
        this.days = days;
    }
}
