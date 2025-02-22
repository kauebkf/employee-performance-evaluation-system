package com.example.service;

import com.example.dto.DepartmentSummary;
import com.example.dto.PeerComparison;
import com.example.dto.PerformanceReport;
import com.example.dto.PerformanceReviewRequest;
import com.example.dto.SubmissionResponse;
import com.example.model.EmployeeInfo;
import com.example.model.PerformanceMetrics;
import com.example.model.PerformanceReview;
import com.example.repository.PerformanceReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PerformanceReviewServiceTest {

    @Mock
    private PerformanceReviewRepository repository;

    @InjectMocks
    private PerformanceReviewService service;

    private PerformanceReviewRequest createRequest(String employeeId, String reviewerId, PerformanceMetrics metrics) {
        PerformanceReviewRequest request = new PerformanceReviewRequest();
        request.setEmployeeId(employeeId);
        request.setReviewerId(reviewerId);
        request.setDepartment("dev_dept");
        request.setRole("developer");
        request.setReviewDate(LocalDate.now().toString());
        request.setMetrics(metrics);
        request.setComments("Great performance!");

        EmployeeInfo employeeInfo = new EmployeeInfo();
        employeeInfo.setDepartmentId("dev_dept");
        employeeInfo.setRole("developer");
        request.setEmployeeInfo(employeeInfo);

        return request;
    }

    private PerformanceMetrics createMetrics(double goalAchievement, double skillLevel, double teamwork) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setGoalAchievement(goalAchievement);
        metrics.setSkillLevel(skillLevel);
        metrics.setTeamwork(teamwork);
        return metrics;
    }

    private PerformanceReview createReview(String employeeId, double score) {
        PerformanceReview review = new PerformanceReview();
        review.setEmployeeId(employeeId);
        review.setReviewerId("reviewer1");
        review.setReviewDate(LocalDate.now());
        review.setMetrics(createMetrics(score, score, score));
        review.calculateOverallScore();
        
        // Set employee info
        EmployeeInfo employeeInfo = new EmployeeInfo();
        employeeInfo.setDepartmentId("dev_dept");
        employeeInfo.setRole("developer");
        review.setEmployeeInfo(employeeInfo);
        
        return review;
    }

    private PerformanceReviewRepository.AggregationResult createPeerResult(String id, double avgScore) {
        return new PerformanceReviewRepository.AggregationResult(id, avgScore);
    }

    private PerformanceReviewRepository.DepartmentResult createDepartmentResult(String id, double avgScore, String role) {
        // Round the score to 2 decimal places to match service implementation
        double roundedScore = Math.round(avgScore * 100.0) / 100.0;
        return new PerformanceReviewRepository.DepartmentResult(id, roundedScore, role);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 1. Review Submission Tests
    @Test
    void submitReview_ValidRequest_ReturnsCorrectResponse() {
        // Given
        PerformanceMetrics metrics = createMetrics(85.0, 90.0, 95.0);
        PerformanceReviewRequest request = createRequest("emp1", "reviewer1", metrics);
        
        // Expected weighted score: (85.0 * 0.4) + (90.0 * 0.3) + (95.0 * 0.3) = 89.5
        double expectedScore = (85.0 * 0.4) + (90.0 * 0.3) + (95.0 * 0.3);
        PerformanceReview savedReview = createReview("emp1", expectedScore);
        savedReview.setId("review1");

        when(repository.save(any(PerformanceReview.class))).thenReturn(savedReview);

        // When
        SubmissionResponse response = service.submitReview(request);

        // Then
        assertNotNull(response);
        assertEquals("review1", response.getReviewId());
        assertEquals("submitted", response.getStatus());
        
        // Verify the review was saved with correct data
        verify(repository).save(argThat(review -> {
            assertEquals("emp1", review.getEmployeeId());
            assertEquals("reviewer1", review.getReviewerId());
            assertEquals("dev_dept", review.getEmployeeInfo().getDepartmentId());
            assertEquals("developer", review.getEmployeeInfo().getRole());
            assertEquals(expectedScore, review.getOverallScore(), 0.01);
            return true;
        }));
    }

    @Test
    void submitReview_NullEmployeeId_ThrowsException() {
        PerformanceMetrics metrics = createMetrics(85, 90, 95);
        PerformanceReviewRequest request = createRequest(null, "reviewer1", metrics);
        assertThrows(IllegalArgumentException.class, () -> service.submitReview(request));
    }

    @Test
    void submitReview_NullReviewerId_ThrowsException() {
        PerformanceMetrics metrics = createMetrics(85, 90, 95);
        PerformanceReviewRequest request = createRequest("emp1", null, metrics);
        assertThrows(IllegalArgumentException.class, () -> service.submitReview(request));
    }

    @Test
    void submitReview_NullMetrics_ThrowsException() {
        PerformanceReviewRequest request = createRequest("emp1", "reviewer1", null);
        assertThrows(IllegalArgumentException.class, () -> service.submitReview(request));
    }

    // 2. Score Calculation Tests
    @Test
    void calculateOverallScore_ValidMetrics_CalculatesCorrectly() {
        PerformanceMetrics metrics = createMetrics(100, 100, 100);
        PerformanceReview review = createReview("emp1", 0);
        review.setMetrics(metrics);
        review.calculateOverallScore();
        assertEquals(100.0, review.getOverallScore(), 0.01);
    }

    @Test
    void calculateOverallScore_WeightedCalculation_IsCorrect() {
        PerformanceMetrics metrics = createMetrics(80, 90, 70);
        PerformanceReview review = createReview("emp1", 0);
        review.setMetrics(metrics);
        review.calculateOverallScore();
        // Expected: (80 * 0.4) + (90 * 0.3) + (70 * 0.3) = 80
        assertEquals(80.0, review.getOverallScore(), 0.01);
    }

    @Test
    void validateMetrics_InvalidRange_ThrowsException() {
        PerformanceMetrics metrics = createMetrics(101, 90, 85);
        PerformanceReview review = createReview("emp1", 0);
        review.setMetrics(metrics);
        assertThrows(IllegalArgumentException.class, review::calculateOverallScore);
    }

    // 3. Employee Performance Report Tests
    @Test
    void getEmployeePerformance_WithMultipleReviews_ReturnsCorrectReport() {
        // Given
        String employeeId = "emp1";
        LocalDate now = LocalDate.now();
        
        // Create reviews with different weighted scores
        PerformanceReview review1 = createReview(employeeId, 0);
        review1.setMetrics(createMetrics(80.0, 90.0, 85.0)); // (80*0.4 + 90*0.3 + 85*0.3) = 84.5
        review1.calculateOverallScore();
        review1.setReviewDate(now.minusDays(1)); // Yesterday
        
        PerformanceReview review2 = createReview(employeeId, 0);
        review2.setMetrics(createMetrics(90.0, 85.0, 95.0)); // (90*0.4 + 85*0.3 + 95*0.3) = 90.0
        review2.calculateOverallScore();
        review2.setReviewDate(now); // Today
        
        PerformanceReview review3 = createReview(employeeId, 0);
        review3.setMetrics(createMetrics(75.0, 80.0, 70.0)); // (75*0.4 + 80*0.3 + 70*0.3) = 75.0
        review3.calculateOverallScore();
        review3.setReviewDate(now.minusDays(2)); // Two days ago

        List<PerformanceReview> allReviews = Arrays.asList(review1, review2, review3);

        when(repository.findByEmployeeId(employeeId)).thenReturn(allReviews);
        when(repository.findByEmployeeIdAndReviewDateBetween(eq(employeeId), any(), any()))
            .thenReturn(Arrays.asList(review1, review2)); // Last quarter reviews

        // When
        PerformanceReport report = service.getEmployeePerformance(employeeId);

        // Then
        assertNotNull(report);
        assertEquals(employeeId, report.getEmployeeId());
        assertEquals(83.17, report.getAverageScore(), 0.01); // (84.5 + 90.0 + 75.0) / 3
        assertEquals("dev_dept", report.getDepartmentId());
        assertEquals(87.25, report.getTrends().getLastQuarter(), 0.01); // (84.5 + 90.0) / 2
        assertEquals(3, report.getReviews().size());
        
        // Verify individual reviews are sorted by date (newest first)
        var reviews = report.getReviews();
        assertEquals(90.0, reviews.get(0).getOverallScore(), 0.01); // Today's review
        assertEquals(84.5, reviews.get(1).getOverallScore(), 0.01); // Yesterday's review
        assertEquals(75.0, reviews.get(2).getOverallScore(), 0.01); // Two days ago review
        
        // Verify metrics details of the latest review
        var latestMetrics = reviews.get(0).getMetrics();
        assertEquals(90.0, latestMetrics.getGoalAchievement(), 0.01);
        assertEquals(85.0, latestMetrics.getSkillLevel(), 0.01);
        assertEquals(95.0, latestMetrics.getTeamwork(), 0.01);
    }

    @Test
    void getEmployeePerformance_NoReviews_ThrowsException() {
        String employeeId = "emp1";
        when(repository.findByEmployeeId(employeeId)).thenReturn(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> service.getEmployeePerformance(employeeId));
    }

    // 4. Peer Comparison Tests
    @Test
    void getPeerComparison_WithTopPerformer_Returns100thPercentile() {
        // Given
        String employeeId = "emp1";
        List<PerformanceReview> reviews = Collections.singletonList(createReview(employeeId, 95.0));

        // Create peer results with weighted scores
        // Each score is weighted: 40% goal achievement, 30% skill level, 30% teamwork
        List<PerformanceReviewRepository.AggregationResult> peerResults = Arrays.asList(
            createPeerResult("emp1", 95.0), // 95 * 0.4 + 95 * 0.3 + 95 * 0.3 = 95.0
            createPeerResult("emp2", 85.0), // 85 * 0.4 + 85 * 0.3 + 85 * 0.3 = 85.0
            createPeerResult("emp3", 75.0)  // 75 * 0.4 + 75 * 0.3 + 75 * 0.3 = 75.0
        );

        when(repository.findByEmployeeId(employeeId)).thenReturn(reviews);
        when(repository.getPeerAggregation(eq("dev_dept"), eq("developer"))).thenReturn(peerResults);

        // When
        PeerComparison comparison = service.getPeerComparison(employeeId);

        // Then
        assertNotNull(comparison);
        assertEquals(employeeId, comparison.getEmployeeId());
        assertEquals("dev_dept", comparison.getDepartmentId());
        assertEquals("developer", comparison.getRole());
        assertEquals(95.0, comparison.getAverageScore());
        assertEquals(100.0, comparison.getPercentileRank());
        // Peer average should exclude the current employee (only emp2 and emp3)
        assertEquals(80.0, comparison.getPeerAverageScore()); // (85.0 + 75.0) / 2 = 80.0
    }

    @Test
    void getPeerComparison_WithMedianPerformer_Returns50thPercentile() {
        String employeeId = "emp2";
        List<PerformanceReview> reviews = Collections.singletonList(createReview(employeeId, 85.0));

        // Create peer results with weighted scores
        // Each score is weighted: 40% goal achievement, 30% skill level, 30% teamwork
        List<PerformanceReviewRepository.AggregationResult> peerResults = Arrays.asList(
            createPeerResult("emp1", 95.0), // 95 * 0.4 + 95 * 0.3 + 95 * 0.3 = 95.0
            createPeerResult("emp2", 85.0), // 85 * 0.4 + 85 * 0.3 + 85 * 0.3 = 85.0
            createPeerResult("emp3", 75.0)  // 75 * 0.4 + 75 * 0.3 + 75 * 0.3 = 75.0
        );

        when(repository.findByEmployeeId(employeeId)).thenReturn(reviews);
        when(repository.getPeerAggregation(eq("dev_dept"), eq("developer"))).thenReturn(peerResults);

        PeerComparison comparison = service.getPeerComparison(employeeId);

        assertNotNull(comparison);
        assertEquals(employeeId, comparison.getEmployeeId());
        assertEquals("dev_dept", comparison.getDepartmentId());
        assertEquals("developer", comparison.getRole());
        assertEquals(85.0, comparison.getAverageScore(), 0.01);
        assertEquals(50.0, comparison.getPercentileRank(), 0.01);
        // Peer average should exclude the current employee (only emp1 and emp3)
        assertEquals(85.0, comparison.getPeerAverageScore(), 0.01); // (95.0 + 75.0) / 2 = 85.0
    }

    @Test
    void getPeerComparison_SingleEmployee_HandlesCorrectly() {
        String employeeId = "emp1";
        
        // Create reviews for the employee
        List<PerformanceReview> reviews = Collections.singletonList(
            createReview(employeeId, 85.0)
        );

        // Create peer results including both the employee and another peer
        List<PerformanceReviewRepository.AggregationResult> peerResults = Arrays.asList(
            createPeerResult("emp1", 85.0),
            createPeerResult("emp2", 85.0)  // Add another peer with same score
        );

        when(repository.findByEmployeeId(employeeId)).thenReturn(reviews);
        when(repository.getPeerAggregation(eq("dev_dept"), eq("developer"))).thenReturn(peerResults);

        PeerComparison comparison = service.getPeerComparison(employeeId);

        // Print actual values for debugging
        System.out.println("Expected employeeId: " + employeeId);
        System.out.println("Actual employeeId: " + comparison.getEmployeeId());
        System.out.println("Expected averageScore: 85.0");
        System.out.println("Actual averageScore: " + comparison.getAverageScore());
        System.out.println("Expected peerAverageScore: 85.0");
        System.out.println("Actual peerAverageScore: " + comparison.getPeerAverageScore());
        System.out.println("Expected percentileRank: 100.0");
        System.out.println("Actual percentileRank: " + comparison.getPercentileRank());

        assertEquals(85.0, comparison.getAverageScore(), 0.01);
        assertEquals(100.0, comparison.getPercentileRank(), 0.01);  // Should be 100th percentile since peer has same score
        assertEquals(85.0, comparison.getPeerAverageScore(), 0.01);
        assertEquals("dev_dept", comparison.getDepartmentId());
        assertEquals("developer", comparison.getRole());
    }

    // 5. Department Summary Tests
    @Test
    void getDepartmentSummary_WithSingleEmployee_HandlesCorrectly() {
        String departmentId = "dev_dept";
        
        // Create department result with weighted score
        // Score is weighted: 40% goal achievement, 30% skill level, 30% teamwork
        // Using 85.0 for all metrics: (85*0.4 + 85*0.3 + 85*0.3) = 85.0
        List<PerformanceReviewRepository.DepartmentResult> results = Collections.singletonList(
            createDepartmentResult("emp1", 85.0, "developer")
        );

        // Set up and verify mock
        when(repository.getDepartmentAggregation(departmentId)).thenReturn(results);

        DepartmentSummary summary = service.getDepartmentSummary(departmentId);

        // Verify mock was called
        verify(repository).getDepartmentAggregation(departmentId);
        
        // Print actual values for debugging
        System.out.println("Expected departmentId: " + departmentId);
        System.out.println("Actual departmentId: " + summary.getDepartmentId());
        System.out.println("Expected averageScore: 85.0");
        System.out.println("Actual averageScore: " + summary.getAverageScore());
        System.out.println("Mock results size: " + results.size());
        System.out.println("Mock first result id: " + results.get(0).getId());
        System.out.println("Mock first result score: " + results.get(0).getAvgScore());
        
        // Verify summary fields
        assertNotNull(summary, "Summary should not be null");
        assertEquals(departmentId, summary.getDepartmentId(), "Department ID should match");
        assertEquals(85.0, summary.getAverageScore(), 0.01, "Average score should be 85.0");
        
        // Verify top performers
        assertNotNull(summary.getTopPerformers(), "Top performers list should not be null");
        assertEquals(1, summary.getTopPerformers().size(), "Should have 1 top performer");
        
        // Verify low performers
        assertNotNull(summary.getLowPerformers(), "Low performers list should not be null");
        assertEquals(0, summary.getLowPerformers().size(), "Should have 0 low performers");
        
        // Verify top performer details
        DepartmentSummary.EmployeePerformance topPerformer = summary.getTopPerformers().get(0);
        assertNotNull(topPerformer, "Top performer should not be null");
        assertEquals("emp1", topPerformer.getEmployeeId(), "Top performer ID should be emp1");
        assertEquals(85.0, topPerformer.getOverallScore(), 0.01, "Top performer score should be 85.0");
        assertEquals(Integer.valueOf(1), topPerformer.getRank(), "Top performer rank should be 1");
    }

    @Test
    void getDepartmentSummary_WithManyEmployees_LimitsTopAndLowPerformers() {
        String departmentId = "dev_dept";
        
        // Create department results with weighted scores
        List<PerformanceReviewRepository.DepartmentResult> results = Arrays.asList(
            createDepartmentResult("emp1", 95.0, "developer"), // Top performer
            createDepartmentResult("emp2", 90.0, "developer"), // Top performer
            createDepartmentResult("emp3", 85.0, "developer"), // Low performer
            createDepartmentResult("emp4", 80.0, "developer"), // Low performer
            createDepartmentResult("emp5", 75.0, "developer"), // Low performer
            createDepartmentResult("emp6", 70.0, "developer")  // Low performer
        );

        when(repository.getDepartmentAggregation(departmentId)).thenReturn(results);

        DepartmentSummary summary = service.getDepartmentSummary(departmentId);

        assertEquals(departmentId, summary.getDepartmentId());
        assertEquals(82.5, summary.getAverageScore(), 0.01);
        assertEquals(2, summary.getTopPerformers().size());
        assertEquals(4, summary.getLowPerformers().size());
        
        // Check top performers (sorted by score)
        DepartmentSummary.EmployeePerformance topPerformer1 = summary.getTopPerformers().get(0);
        assertEquals("emp1", topPerformer1.getEmployeeId());
        assertEquals(95.0, topPerformer1.getOverallScore(), 0.01);
        assertEquals(1, topPerformer1.getRank().intValue());
        
        DepartmentSummary.EmployeePerformance topPerformer2 = summary.getTopPerformers().get(1);
        assertEquals("emp2", topPerformer2.getEmployeeId());
        assertEquals(90.0, topPerformer2.getOverallScore(), 0.01);
        assertEquals(2, topPerformer2.getRank().intValue());

        // Check low performers (sorted by score)
        DepartmentSummary.EmployeePerformance lowPerformer1 = summary.getLowPerformers().get(0);
        assertEquals("emp3", lowPerformer1.getEmployeeId());
        assertEquals(85.0, lowPerformer1.getOverallScore(), 0.01);
        assertNull(lowPerformer1.getRank());
        
        DepartmentSummary.EmployeePerformance lowPerformer4 = summary.getLowPerformers().get(3);
        assertEquals("emp6", lowPerformer4.getEmployeeId());
        assertEquals(70.0, lowPerformer4.getOverallScore(), 0.01);
        assertNull(lowPerformer4.getRank());
    }

    @Test
    void getDepartmentSummary_NoLowPerformers_HandlesCorrectly() {
        String departmentId = "dev_dept";
        
        // Create department results with all high scores
        List<PerformanceReviewRepository.DepartmentResult> results = Arrays.asList(
            createDepartmentResult("emp1", 95.0, "developer"),
            createDepartmentResult("emp2", 90.0, "developer")  // Only 2 employees
        );

        when(repository.getDepartmentAggregation(departmentId)).thenReturn(results);

        DepartmentSummary summary = service.getDepartmentSummary(departmentId);

        assertEquals(departmentId, summary.getDepartmentId());
        assertEquals(92.5, summary.getAverageScore(), 0.01);
        assertEquals(2, summary.getTopPerformers().size());
        assertTrue(summary.getLowPerformers().isEmpty());
        
        // Verify top performers order
        var topPerformers = summary.getTopPerformers();
        assertEquals("emp1", topPerformers.get(0).getEmployeeId());
        assertEquals(95.0, topPerformers.get(0).getOverallScore(), 0.01);
        assertEquals(Integer.valueOf(1), topPerformers.get(0).getRank());
        
        assertEquals("emp2", topPerformers.get(1).getEmployeeId());
        assertEquals(90.0, topPerformers.get(1).getOverallScore(), 0.01);
        assertEquals(Integer.valueOf(2), topPerformers.get(1).getRank());
    }

    @Test
    void getDepartmentSummary_NoReviews_ThrowsException() {
        String departmentId = "dev_dept";
        when(repository.getDepartmentAggregation(departmentId)).thenReturn(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> service.getDepartmentSummary(departmentId));
    }

    // 6. Edge Cases and Validation Tests
    @Test
    void validateMetrics_NegativeValue_ThrowsException() {
        PerformanceMetrics metrics = createMetrics(-1, 90, 85);
        PerformanceReview review = createReview("emp1", 0);
        review.setMetrics(metrics);
        assertThrows(IllegalArgumentException.class, review::calculateOverallScore);
    }

    @Test
    void validateMetrics_NegativeValue_ThrowsExceptionInSubmitReview() {
        PerformanceMetrics metrics = createMetrics(-1, 90, 85);
        PerformanceReviewRequest request = createRequest("emp1", "reviewer1", metrics);
        assertThrows(IllegalArgumentException.class, () -> service.submitReview(request));
    }

    @Test
    void validateMetrics_ValueOver100_ThrowsException() {
        PerformanceMetrics metrics = createMetrics(101, 90, 85);
        PerformanceReviewRequest request = createRequest("emp1", "reviewer1", metrics);
        assertThrows(IllegalArgumentException.class, () -> service.submitReview(request));
    }

    @Test
    void submitReview_NullComments_Accepted() {
        PerformanceMetrics metrics = createMetrics(85, 90, 95);
        PerformanceReviewRequest request = createRequest("emp1", "reviewer1", metrics);
        request.setComments(null);
        
        PerformanceReview savedReview = createReview("emp1", 89.5);
        savedReview.setId("review1");
        when(repository.save(any(PerformanceReview.class))).thenReturn(savedReview);

        SubmissionResponse response = service.submitReview(request);
        assertNotNull(response);
        assertEquals("review1", response.getReviewId());
    }

    @Test
    void getDepartmentSummary_NoLowPerformers_HandlesCorrectly() {
        String departmentId = "dev_dept";
        
        // Create department results with all high scores
        List<PerformanceReviewRepository.DepartmentResult> results = Arrays.asList(
            createDepartmentResult("emp1", 95.0, "developer"),
            createDepartmentResult("emp2", 90.0, "developer"),
            createDepartmentResult("emp3", 92.0, "developer")
        );

        when(repository.getDepartmentAggregation(departmentId)).thenReturn(results);

        DepartmentSummary summary = service.getDepartmentSummary(departmentId);

        assertEquals(departmentId, summary.getDepartmentId());
        assertEquals(92.33, summary.getAverageScore(), 0.01);
        assertEquals(2, summary.getTopPerformers().size());
        assertTrue(summary.getLowPerformers().isEmpty());
    }
}
