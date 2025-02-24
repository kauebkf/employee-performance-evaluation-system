package com.example.dto;

import com.example.model.PerformanceMetrics;
import com.example.model.EmployeeInfo;
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
    
    @NotNull(message = "Performance metrics are required")
    private PerformanceMetrics metrics;
    
    @NotNull(message = "Employee info is required")
    private EmployeeInfo employeeInfo;
    
    private String reviewDate;
    
    private String comments;
}
