package com.example.service;

import com.example.dto.DepartmentSummary;
import com.example.dto.PeerComparison;
import com.example.dto.PerformanceReport;
import com.example.dto.SubmissionResponse;
import com.example.dto.PerformanceReviewRequest;
import com.example.model.EmployeeInfo;
import com.example.model.PerformanceReview;
import com.example.repository.PerformanceReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerformanceReviewService {

    private final PerformanceReviewRepository repository;

    public PerformanceReviewService(PerformanceReviewRepository repository) {
        this.repository = repository;
    }

    public SubmissionResponse submitReview(PerformanceReviewRequest request) {
        // Validate request
        if (request.getEmployeeId() == null || request.getReviewerId() == null) {
            throw new IllegalArgumentException("Missing required fields: employeeId or reviewerId");
        }
        if (request.getMetrics() == null) {
            throw new IllegalArgumentException("Missing required field: metrics");
        }
        if (request.getEmployeeInfo() == null) {
            throw new IllegalArgumentException("Missing required field: employeeInfo");
        }
        if (request.getDepartment() == null || request.getRole() == null) {
            throw new IllegalArgumentException("Missing required fields: department or role");
        }

        // Create new review from request
        PerformanceReview review = new PerformanceReview();
        review.setEmployeeId(request.getEmployeeId());
        review.setReviewerId(request.getReviewerId());
        review.setReviewDate(LocalDate.parse(request.getReviewDate()));
        review.setMetrics(request.getMetrics());
        review.setComments(request.getComments());

        // Set employee info from request
        EmployeeInfo employeeInfo = request.getEmployeeInfo();
        employeeInfo.setDepartmentId(request.getDepartment()); // Override with latest department
        employeeInfo.setRole(request.getRole()); // Override with latest role
        review.setEmployeeInfo(employeeInfo);

        review.calculateOverallScore(); // This will validate metrics range

        // Save review
        PerformanceReview savedReview = repository.save(review);

        // Return response
        return new SubmissionResponse(savedReview.getId(), "submitted");
    }

    public PerformanceReport getEmployeePerformance(String employeeId) {
        List<PerformanceReview> allReviews = repository.findByEmployeeId(employeeId);
        if (allReviews.isEmpty()) {
            throw new IllegalArgumentException("No reviews found for employee: " + employeeId);
        }

        // Get latest review for department info
        PerformanceReview latestReview = allReviews.stream()
                .max(Comparator.comparing(PerformanceReview::getReviewDate))
                .orElseThrow();

        // Calculate overall average
        double averageScore = allReviews.stream()
                .mapToDouble(PerformanceReview::getOverallScore)
                .average()
                .orElse(0.0);

        // Calculate trends
        LocalDate now = LocalDate.now();
        LocalDate quarterAgo = now.minusMonths(3);
        LocalDate yearAgo = now.minusYears(1);

        // Get reviews within last quarter
        List<PerformanceReview> quarterReviews = repository.findByEmployeeIdAndReviewDateBetween(
                employeeId, quarterAgo, now);
        double quarterAverage = quarterReviews.stream()
                .mapToDouble(PerformanceReview::getOverallScore)
                .average()
                .orElse(0.0);

        // Get reviews within last year
        List<PerformanceReview> yearReviews = repository.findByEmployeeIdAndReviewDateBetween(
                employeeId, yearAgo, now);
        double yearAverage = yearReviews.stream()
                .mapToDouble(PerformanceReview::getOverallScore)
                .average()
                .orElse(0.0);

        // Convert reviews to DTO format
        List<PerformanceReport.Review> reviewDTOs = allReviews.stream()
                .sorted(Comparator.comparing(PerformanceReview::getReviewDate).reversed())
                .map(review -> {
                    PerformanceReport.Review dto = new PerformanceReport.Review();
                    dto.setReviewDate(review.getReviewDate());
                    dto.setMetrics(review.getMetrics());
                    dto.setComments(review.getComments());
                    dto.setOverallScore(review.getOverallScore());
                    return dto;
                })
                .collect(Collectors.toList());

        // Create and return report
        PerformanceReport report = new PerformanceReport();
        report.setEmployeeId(employeeId);
        report.setDepartmentId(latestReview.getEmployeeInfo().getDepartmentId());
        report.setAverageScore(Math.round(averageScore * 100.0) / 100.0);
        report.setReviews(reviewDTOs);
        report.setTrends(new PerformanceReport.Trends(Math.round(quarterAverage * 100.0) / 100.0, Math.round(yearAverage * 100.0) / 100.0));
        return report;
    }

    public PeerComparison getPeerComparison(String employeeId) {
        List<PerformanceReview> reviews = repository.findByEmployeeId(employeeId);
        if (reviews.isEmpty()) {
            throw new IllegalArgumentException("No reviews found for employee: " + employeeId);
        }

        // Get the employee's latest review for current role and department
        PerformanceReview latestReview = reviews.stream()
                .max(Comparator.comparing(PerformanceReview::getReviewDate))
                .orElseThrow();

        // Calculate employee's average score
        double employeeAvgScore = reviews.stream()
                .mapToDouble(PerformanceReview::getOverallScore)
                .average()
                .orElse(0.0);

        // Get peer scores
        String departmentId = latestReview.getEmployeeInfo().getDepartmentId();
        String role = latestReview.getEmployeeInfo().getRole();
        List<PerformanceReviewRepository.AggregationResult> peers = repository.getPeerAggregation(departmentId, role);

        // Calculate peer average (excluding the current employee)
        List<PerformanceReviewRepository.AggregationResult> peerScores = peers.stream()
                .filter(p -> !p.getId().equals(employeeId))
                .collect(Collectors.toList());

        // Calculate peer average
        double peerAverage = peerScores.stream()
                .mapToDouble(PerformanceReviewRepository.AggregationResult::getAvgScore)
                .average()
                .orElse(0.0);

        // Calculate percentile rank
        long peersBelow = peerScores.stream()
                .filter(p -> p.getAvgScore() <= employeeAvgScore)  
                .count();

        double percentileRank = peerScores.isEmpty() ? 100.0 : 
                               (double) peersBelow / peerScores.size() * 100.0;

        // Create response with rounded scores
        PeerComparison comparison = new PeerComparison();
        comparison.setEmployeeId(employeeId);
        comparison.setDepartmentId(departmentId);
        comparison.setRole(role);
        comparison.setAverageScore(Math.round(employeeAvgScore * 100.0) / 100.0);
        comparison.setPercentileRank(Math.round(percentileRank * 100.0) / 100.0);
        comparison.setPeerAverageScore(Math.round(peerAverage * 100.0) / 100.0);

        return comparison;
    }

    public DepartmentSummary getDepartmentSummary(String departmentId) {
        List<PerformanceReviewRepository.DepartmentResult> results = 
                repository.getDepartmentAggregation(departmentId);

        if (results.isEmpty()) {
            throw new IllegalArgumentException("No reviews found for department: " + departmentId);
        }

        // Calculate department average
        double departmentAverage = results.stream()
                .mapToDouble(PerformanceReviewRepository.DepartmentResult::getAvgScore)
                .average()
                .orElse(0.0);

        // Sort all results by score in descending order
        List<PerformanceReviewRepository.DepartmentResult> sortedResults = results.stream()
                .sorted(Comparator.comparingDouble(PerformanceReviewRepository.DepartmentResult::getAvgScore).reversed())
                .collect(Collectors.toList());

        // Take top 2 performers
        List<DepartmentSummary.EmployeePerformance> topPerformers = sortedResults.stream()
                .limit(2)
                .map(result -> new DepartmentSummary.EmployeePerformance(
                        result.getId(),
                        Math.round(result.getAvgScore() * 100.0) / 100.0,
                        sortedResults.indexOf(result) + 1))
                .collect(Collectors.toList());

        // All others are low performers
        List<DepartmentSummary.EmployeePerformance> lowPerformers = sortedResults.stream()
                .skip(2) // Skip the top 2
                .map(result -> new DepartmentSummary.EmployeePerformance(
                        result.getId(),
                        Math.round(result.getAvgScore() * 100.0) / 100.0,
                        null))
                .collect(Collectors.toList());

        // Create and return summary
        DepartmentSummary summary = new DepartmentSummary();
        summary.setDepartmentId(departmentId);
        summary.setAverageScore(Math.round(departmentAverage * 100.0) / 100.0);
        summary.setTopPerformers(topPerformers);
        summary.setLowPerformers(lowPerformers);

        return summary;
    }
}
