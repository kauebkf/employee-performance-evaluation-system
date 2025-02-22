package com.example.dto;

import lombok.Data;
import java.util.List;

@Data
public class DepartmentSummary {
    private String departmentId;
    private double averageScore;
    private List<PerformerInfo> topPerformers;
    private List<PerformerInfo> lowPerformers;

    @Data
    public static class PerformerInfo {
        private String employeeId;
        private double overallScore;
        private Integer rank;
    }
}
