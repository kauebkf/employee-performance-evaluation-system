package com.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;
import com.example.model.PerformanceMetrics;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReport {
    private String employeeId;
    private double averageScore;
    private String departmentId;
    private List<Review> reviews;
    private Trends trends;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private LocalDate reviewDate;
        private PerformanceMetrics metrics;
        private String comments;
        private double overallScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trends {
        private double lastQuarter;
        private double lastYear;
    }
}
