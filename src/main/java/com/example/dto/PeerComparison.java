package com.example.dto;

import lombok.Data;

@Data
public class PeerComparison {
    private String employeeId;
    private String departmentId;
    private String role;
    private double averageScore;
    private double percentileRank;
    private double peerAverageScore;
}
