package com.example.controller;

import com.example.dto.DepartmentSummary;
import com.example.dto.PeerComparison;
import com.example.dto.PerformanceReport;
import com.example.dto.PerformanceReviewRequest;
import com.example.dto.SubmissionResponse;
import com.example.service.PerformanceReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PerformanceReviewController {

    @Autowired
    private PerformanceReviewService service;

    @PostMapping("/reviews")
    public ResponseEntity<SubmissionResponse> submitReview(@Valid @RequestBody PerformanceReviewRequest request) {
        return ResponseEntity.ok(service.submitReview(request));
    }

    @GetMapping("/employees/{employeeId}/performance")
    public ResponseEntity<PerformanceReport> getEmployeePerformance(@PathVariable String employeeId) {
        return ResponseEntity.ok(service.getEmployeePerformance(employeeId));
    }

    @GetMapping("/employees/{employeeId}/peer-comparison")
    public ResponseEntity<PeerComparison> getPeerComparison(@PathVariable String employeeId) {
        return ResponseEntity.ok(service.getPeerComparison(employeeId));
    }

    @GetMapping("/departments/{departmentId}/performance-summary")
    public ResponseEntity<DepartmentSummary> getDepartmentSummary(@PathVariable String departmentId) {
        return ResponseEntity.ok(service.getDepartmentSummary(departmentId));
    }
}
