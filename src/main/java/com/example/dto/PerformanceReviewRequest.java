package com.example.dto;

import com.example.model.PerformanceMetrics;
import lombok.Data;

@Data
public class PerformanceReviewRequest {
    private String employeeId;
    private String reviewerId;
    private PerformanceMetrics metrics;
    private String comments;
}
