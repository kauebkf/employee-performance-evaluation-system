package com.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "performance_reviews")
public class PerformanceReview {
    @Id
    private String id;
    private String employeeId;
    private String reviewerId;
    private LocalDate reviewDate;
    private PerformanceMetrics metrics;
    private EmployeeInfo employeeInfo;
    private String comments;
    private double overallScore;

    public void calculateOverallScore() {
        validateMetricRange(metrics.getGoalAchievement(), "Goal Achievement");
        validateMetricRange(metrics.getSkillLevel(), "Skill Level");
        validateMetricRange(metrics.getTeamwork(), "Teamwork");

        this.overallScore = (metrics.getGoalAchievement() * 0.4) +
                          (metrics.getSkillLevel() * 0.3) +
                          (metrics.getTeamwork() * 0.3);
    }

    private void validateMetricRange(double value, String metricName) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(metricName + " must be between 0 and 100");
        }
    }
}
