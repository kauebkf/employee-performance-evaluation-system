package com.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    @Min(value = 0, message = "Goal achievement must be at least 0")
    @Max(value = 100, message = "Goal achievement cannot exceed 100")
    private double goalAchievement;

    @Min(value = 0, message = "Skill level must be at least 0")
    @Max(value = 100, message = "Skill level cannot exceed 100")
    private double skillLevel;

    @Min(value = 0, message = "Teamwork must be at least 0")
    @Max(value = 100, message = "Teamwork cannot exceed 100")
    private double teamwork;
}
