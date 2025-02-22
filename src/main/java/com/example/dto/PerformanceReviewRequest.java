package com.example.dto;

import com.example.model.PerformanceMetrics;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReviewRequest {
    @NotNull(message = "Employee ID is required")
    private String employeeId;
    
    @NotNull(message = "Reviewer ID is required")
    private String reviewerId;
    
    @NotNull(message = "Department is required")
    private String department;
    
    @NotNull(message = "Role is required")
    private String role;
    
    @NotNull(message = "Review date is required")
    private String reviewDate;
    
    @NotNull(message = "Performance metrics are required")
    private PerformanceMetrics metrics;
    
    private String comments;
}
