package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Data
@Document(collection = "performance_reviews")
public class PerformanceReview {
    @Id
    private String id;
    private String employeeId;
    private String reviewerId;
    private LocalDate reviewDate;
    private EmployeeInfo employeeInfo;
    private PerformanceMetrics metrics;
    private String comments;
    private double overallScore;

    public void calculateOverallScore() {
        if (metrics == null) {
            throw new IllegalStateException("Metrics cannot be null");
        }
        // Validate metrics range
        validateMetricRange(metrics.getGoalAchievement(), "Goal Achievement");
        validateMetricRange(metrics.getSkillLevel(), "Skill Level");
        validateMetricRange(metrics.getTeamwork(), "Teamwork");
        
        // Calculate weighted score
        this.overallScore = (metrics.getGoalAchievement() * 0.4) +
                           (metrics.getSkillLevel() * 0.3) +
                           (metrics.getTeamwork() * 0.3);
    }

    private void validateMetricRange(int value, String metricName) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(
                String.format("%s must be between 0 and 100, got: %d", metricName, value));
        }
    }
}
