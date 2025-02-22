package com.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    private double goalAchievement;
    private double skillLevel;
    private double teamwork;
}
