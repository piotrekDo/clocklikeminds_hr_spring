package com.example.clocklike_portal.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settingId;
    private String settingName;
    private String settingType;
    private String settingValue;

    public Settings(String settingName, String settingType, String settingValue) {
        this.settingName = settingName;
        this.settingType = settingType;
        this.settingValue = settingValue;
    }
}


