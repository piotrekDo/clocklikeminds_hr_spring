package com.example.clocklike_portal.timeoff_history;

import com.example.clocklike_portal.appUser.AppUserEntity;
import com.example.clocklike_portal.timeoff.PtoEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RequestHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;
    private String action;
    private String notes;
    private LocalDateTime dateTime;
    @ManyToOne
    private AppUserEntity appUserEntity;
    @ManyToOne
    private PtoEntity ptoEntity;
}
