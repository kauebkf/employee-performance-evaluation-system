package com.example.dto;

import com.example.model.PerformanceReview;
import lombok.Data;
import java.util.List;

@Data
public class PerformanceReport {
    private String employeeId;
    private double averageScore;
    private String departmentId;
    private List<PerformanceReview> reviews;
    private PerformanceTrends trends;

    @Data
    public static class PerformanceTrends {
        private double lastQuarter;
        private double lastYear;
    }
}
