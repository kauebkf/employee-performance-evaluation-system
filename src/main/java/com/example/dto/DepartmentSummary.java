package com.example.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSummary {
    private String departmentId;
    private double averageScore;
    private List<EmployeePerformance> topPerformers;
    private List<EmployeePerformance> lowPerformers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmployeePerformance {
        private String employeeId;
        private double overallScore;
        private Integer rank; // Optional, only for top performers
    }
}
