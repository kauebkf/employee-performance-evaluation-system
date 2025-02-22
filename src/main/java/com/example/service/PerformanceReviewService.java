package com.example.service;

import com.example.dto.*;
import com.example.model.PerformanceReview;
import com.example.repository.PerformanceReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PerformanceReviewService {
    
    @Autowired
    private PerformanceReviewRepository repository;

    public SubmissionResponse submitReview(PerformanceReviewRequest request) {
        PerformanceReview review = new PerformanceReview();
        review.setEmployeeId(request.getEmployeeId());
        review.setReviewerId(request.getReviewerId());
        review.setReviewDate(LocalDate.now());
        review.setMetrics(request.getMetrics());
        review.setComments(request.getComments());
        review.calculateOverallScore();
        
        review = repository.save(review);
        return SubmissionResponse.of(review.getId());
    }

    public PerformanceReport getEmployeePerformance(String employeeId) {
        List<PerformanceReview> reviews = repository.findByEmployeeId(employeeId);
        if (reviews.isEmpty()) {
            throw new RuntimeException("No reviews found for employee: " + employeeId);
        }

        PerformanceReport report = new PerformanceReport();
        report.setEmployeeId(employeeId);
        report.setDepartmentId(reviews.get(0).getEmployeeInfo().getDepartmentId());
        report.setReviews(reviews);
        
        double averageScore = reviews.stream()
            .mapToDouble(PerformanceReview::getOverallScore)
            .average()
            .orElse(0.0);
        report.setAverageScore(averageScore);

        // Calculate trends
        LocalDate now = LocalDate.now();
        LocalDate quarterAgo = now.minusMonths(3);
        LocalDate yearAgo = now.minusYears(1);

        List<PerformanceReview> quarterReviews = repository.findByEmployeeIdAndReviewDateBetween(
            employeeId, quarterAgo, now);
        List<PerformanceReview> yearReviews = repository.findByEmployeeIdAndReviewDateBetween(
            employeeId, yearAgo, now);

        PerformanceReport.PerformanceTrends trends = new PerformanceReport.PerformanceTrends();
        trends.setLastQuarter(calculateAverage(quarterReviews));
        trends.setLastYear(calculateAverage(yearReviews));
        report.setTrends(trends);

        return report;
    }

    public PeerComparison getPeerComparison(String employeeId) {
        List<PerformanceReview> employeeReviews = repository.findByEmployeeId(employeeId);
        if (employeeReviews.isEmpty()) {
            throw new RuntimeException("No reviews found for employee: " + employeeId);
        }

        PerformanceReview latestReview = employeeReviews.get(0);
        String departmentId = latestReview.getEmployeeInfo().getDepartmentId();
        String role = latestReview.getEmployeeInfo().getRole();

        // Get employee's average score
        double employeeAverage = calculateAverage(employeeReviews);

        // Get peer scores using aggregation
        List<PerformanceReviewRepository.PeerAggregationResult> peerScores = 
            repository.getPeerAggregation(departmentId, role);

        // Calculate peer average
        double peerAverage = peerScores.stream()
            .mapToDouble(PerformanceReviewRepository.PeerAggregationResult::getAvgScore)
            .average()
            .orElse(0.0);

        // Calculate percentile rank using the formula P = (r/N) * 100
        long scoresBelow = peerScores.stream()
            .filter(peer -> peer.getAvgScore() < employeeAverage)
            .count();
        double percentileRank = ((double) scoresBelow / peerScores.size()) * 100;

        PeerComparison comparison = new PeerComparison();
        comparison.setEmployeeId(employeeId);
        comparison.setDepartmentId(departmentId);
        comparison.setRole(role);
        comparison.setAverageScore(employeeAverage);
        comparison.setPeerAverageScore(peerAverage);
        comparison.setPercentileRank(percentileRank);

        return comparison;
    }

    public DepartmentSummary getDepartmentSummary(String departmentId) {
        List<PerformanceReviewRepository.DepartmentAggregationResult> results = 
            repository.getDepartmentAggregation(departmentId);
        
        if (results.isEmpty()) {
            throw new RuntimeException("No reviews found for department: " + departmentId);
        }

        // Calculate department average
        double departmentAverage = results.stream()
            .mapToDouble(PerformanceReviewRepository.DepartmentAggregationResult::getAvgScore)
            .average()
            .orElse(0.0);

        // Sort by score for top and low performers
        var sortedResults = results.stream()
            .sorted(Comparator.comparingDouble(
                PerformanceReviewRepository.DepartmentAggregationResult::getAvgScore).reversed())
            .collect(Collectors.toList());

        DepartmentSummary summary = new DepartmentSummary();
        summary.setDepartmentId(departmentId);
        summary.setAverageScore(departmentAverage);

        // Get top 2 performers
        summary.setTopPerformers(sortedResults.stream()
            .limit(2)
            .map(result -> {
                var info = new DepartmentSummary.PerformerInfo();
                info.setEmployeeId(result.getId());
                info.setOverallScore(result.getAvgScore());
                info.setRank(sortedResults.indexOf(result) + 1);
                return info;
            })
            .collect(Collectors.toList()));

        // Get bottom 2 performers
        summary.setLowPerformers(sortedResults.stream()
            .skip(Math.max(0, sortedResults.size() - 2))
            .map(result -> {
                var info = new DepartmentSummary.PerformerInfo();
                info.setEmployeeId(result.getId());
                info.setOverallScore(result.getAvgScore());
                return info;
            })
            .collect(Collectors.toList()));

        return summary;
    }

    private double calculateAverage(List<PerformanceReview> reviews) {
        return reviews.stream()
            .mapToDouble(PerformanceReview::getOverallScore)
            .average()
            .orElse(0.0);
    }
}
