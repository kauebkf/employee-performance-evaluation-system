package com.example.service;

import com.example.dto.DepartmentSummary;
import com.example.dto.PeerComparison;
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
import static org.mockito.Mockito.when;

class PerformanceReviewServiceTest {

    @Mock
    private PerformanceReviewRepository repository;

    @InjectMocks
    private PerformanceReviewService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private PerformanceReview createReview(String employeeId, double score) {
        PerformanceReview review = new PerformanceReview();
        review.setEmployeeId(employeeId);
        review.setReviewDate(LocalDate.now());
        review.setOverallScore(score);

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
        return new PerformanceReviewRepository.DepartmentResult(id, avgScore, role);
    }

    @Test
    void getPeerComparison_WithTopPerformer_Returns100thPercentile() {
        // Given
        String employeeId = "emp1";
        List<PerformanceReview> reviews = Arrays.asList(
                createReview(employeeId, 95.0)
        );

        List<PerformanceReviewRepository.AggregationResult> peerResults = Arrays.asList(
                createPeerResult("emp1", 95.0),
                createPeerResult("emp2", 85.0),
                createPeerResult("emp3", 75.0)
        );

        when(repository.findByEmployeeId(employeeId)).thenReturn(reviews);
        when(repository.getPeerAggregation(any(), any())).thenReturn(peerResults);

        // When
        PeerComparison comparison = service.getPeerComparison(employeeId);

        // Then
        assertEquals(95.0, comparison.getAverageScore());
        assertEquals(100.0, comparison.getPercentileRank());
        assertEquals(80.0, comparison.getPeerAverageScore());
    }

    @Test
    void getDepartmentSummary_WithTwoEmployees_HandlesBothTopAndLow() {
        // Given
        String departmentId = "dev_dept";
        List<PerformanceReviewRepository.DepartmentResult> results = Arrays.asList(
                createDepartmentResult("emp1", 85.0, "developer"),
                createDepartmentResult("emp2", 65.0, "developer")
        );

        when(repository.getDepartmentAggregation(departmentId)).thenReturn(results);

        // When
        DepartmentSummary summary = service.getDepartmentSummary(departmentId);

        // Then
        assertEquals(75.0, summary.getAverageScore());
        assertEquals(1, summary.getTopPerformers().size());
        assertEquals(1, summary.getLowPerformers().size());
        
        // Check top performer
        assertEquals("emp1", summary.getTopPerformers().get(0).getEmployeeId());
        assertEquals(85.0, summary.getTopPerformers().get(0).getOverallScore());
        assertEquals(1, summary.getTopPerformers().get(0).getRank());

        // Check low performer
        assertEquals("emp2", summary.getLowPerformers().get(0).getEmployeeId());
        assertEquals(65.0, summary.getLowPerformers().get(0).getOverallScore());
        assertNull(summary.getLowPerformers().get(0).getRank());
    }

    @Test
    void getDepartmentSummary_WithSingleEmployee_HandlesCorrectly() {
        // Given
        String departmentId = "dev_dept";
        List<PerformanceReviewRepository.DepartmentResult> results = Collections.singletonList(
                createDepartmentResult("emp1", 85.0, "developer")
        );

        when(repository.getDepartmentAggregation(departmentId)).thenReturn(results);

        // When
        DepartmentSummary summary = service.getDepartmentSummary(departmentId);

        // Then
        assertEquals(85.0, summary.getAverageScore());
        assertEquals(1, summary.getTopPerformers().size());
        assertTrue(summary.getLowPerformers().isEmpty());
        
        // Check top performer
        assertEquals("emp1", summary.getTopPerformers().get(0).getEmployeeId());
        assertEquals(85.0, summary.getTopPerformers().get(0).getOverallScore());
        assertEquals(1, summary.getTopPerformers().get(0).getRank());
    }

    @Test
    void getDepartmentSummary_WithManyEmployees_LimitsTopAndLowPerformers() {
        // Given
        String departmentId = "dev_dept";
        List<PerformanceReviewRepository.DepartmentResult> results = Arrays.asList(
                createDepartmentResult("emp1", 95.0, "developer"),
                createDepartmentResult("emp2", 90.0, "developer"),
                createDepartmentResult("emp3", 85.0, "developer"),
                createDepartmentResult("emp4", 80.0, "developer"),
                createDepartmentResult("emp6", 60.0, "developer"),
                createDepartmentResult("emp5", 65.0, "developer")
        );

        when(repository.getDepartmentAggregation(departmentId)).thenReturn(results);

        // When
        DepartmentSummary summary = service.getDepartmentSummary(departmentId);

        // Then
        assertEquals(79.17, summary.getAverageScore(), 0.01);
        assertEquals(3, summary.getTopPerformers().size());
        assertEquals(2, summary.getLowPerformers().size());
        
        // Check top performers
        assertEquals("emp1", summary.getTopPerformers().get(0).getEmployeeId());
        assertEquals(95.0, summary.getTopPerformers().get(0).getOverallScore());
        assertEquals(1, summary.getTopPerformers().get(0).getRank());
        
        assertEquals("emp2", summary.getTopPerformers().get(1).getEmployeeId());
        assertEquals(90.0, summary.getTopPerformers().get(1).getOverallScore());
        assertEquals(2, summary.getTopPerformers().get(1).getRank());
        
        assertEquals("emp3", summary.getTopPerformers().get(2).getEmployeeId());
        assertEquals(85.0, summary.getTopPerformers().get(2).getOverallScore());
        assertEquals(3, summary.getTopPerformers().get(2).getRank());

        // Check low performers
        assertEquals("emp5", summary.getLowPerformers().get(0).getEmployeeId());
        assertEquals(65.0, summary.getLowPerformers().get(0).getOverallScore());
        assertNull(summary.getLowPerformers().get(0).getRank());
        
        assertEquals("emp6", summary.getLowPerformers().get(1).getEmployeeId());
        assertEquals(60.0, summary.getLowPerformers().get(1).getOverallScore());
        assertNull(summary.getLowPerformers().get(1).getRank());
    }

    @Test
    void getPeerComparison_WithLargeTeam_CalculatesPercentileCorrectly() {
        // Given
        String employeeId = "emp5";
        List<PerformanceReview> reviews = Arrays.asList(
                createReview(employeeId, 85.0)
        );

        List<PerformanceReviewRepository.AggregationResult> peerResults = Arrays.asList(
                createPeerResult("emp1", 95),
                createPeerResult("emp2", 90),
                createPeerResult("emp3", 88),
                createPeerResult("emp4", 86),
                createPeerResult("emp5", 85),
                createPeerResult("emp6", 82),
                createPeerResult("emp7", 80),
                createPeerResult("emp8", 78),
                createPeerResult("emp9", 75),
                createPeerResult("emp10", 70)
        );

        when(repository.findByEmployeeId(employeeId)).thenReturn(reviews);
        when(repository.getPeerAggregation(any(), any())).thenReturn(peerResults);

        // When
        PeerComparison comparison = service.getPeerComparison(employeeId);

        // Then
        assertEquals(85.0, comparison.getAverageScore());
        assertEquals(55.56, comparison.getPercentileRank(), 0.01); // 5 peers below out of 9 total peers
        assertEquals(82.67, comparison.getPeerAverageScore(), 0.01);
    }
}
