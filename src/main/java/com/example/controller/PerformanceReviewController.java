package com.example.controller;

import com.example.dto.*;
import com.example.service.PerformanceReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
