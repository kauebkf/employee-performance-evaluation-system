package com.example.service;

import com.example.dto.PerformanceReviewRequest;
import com.example.dto.SubmissionResponse;
import com.example.model.EmployeeInfo;
import com.example.model.PerformanceMetrics;
import com.example.model.PerformanceReview;
import com.example.repository.PerformanceReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PerformanceReviewServiceTest {

    @Mock
    private PerformanceReviewRepository repository;

    @InjectMocks
    private PerformanceReviewService service;

    private PerformanceReview sampleReview;
    private PerformanceReviewRequest sampleRequest;

    @BeforeEach
    void setUp() {
        // Setup sample metrics
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setGoalAchievement(80);
        metrics.setSkillLevel(90);
        metrics.setTeamwork(85);

        // Setup sample employee info
        EmployeeInfo employeeInfo = new EmployeeInfo();
        employeeInfo.setDepartmentId("dept1");
        employeeInfo.setRole("developer");

        // Setup sample review
        sampleReview = new PerformanceReview();
        sampleReview.setId("1");
        sampleReview.setEmployeeId("123");
        sampleReview.setReviewerId("456");
        sampleReview.setReviewDate(LocalDate.now());
        sampleReview.setMetrics(metrics);
        sampleReview.setEmployeeInfo(employeeInfo);
        sampleReview.setComments("Great job!");
        sampleReview.calculateOverallScore();

        // Setup sample request
        sampleRequest = new PerformanceReviewRequest();
        sampleRequest.setEmployeeId("123");
        sampleRequest.setReviewerId("456");
        sampleRequest.setMetrics(metrics);
        sampleRequest.setComments("Great job!");
    }

    @Test
    void submitReview_ValidRequest_ReturnsCorrectResponse() {
        when(repository.save(any(PerformanceReview.class))).thenReturn(sampleReview);

        SubmissionResponse response = service.submitReview(sampleRequest);

        assertNotNull(response);
        assertEquals("1", response.getReviewId());
        assertEquals("submitted", response.getStatus());
    }

    @Test
    void calculateOverallScore_ValidMetrics_CalculatesCorrectly() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setGoalAchievement(100);
        metrics.setSkillLevel(100);
        metrics.setTeamwork(100);

        PerformanceReview review = new PerformanceReview();
        review.setMetrics(metrics);
        review.calculateOverallScore();

        assertEquals(100.0, review.getOverallScore(), 0.01);
    }

    @Test
    void calculateOverallScore_WeightedCalculation_IsCorrect() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setGoalAchievement(80); // 0.4 weight
        metrics.setSkillLevel(90);      // 0.3 weight
        metrics.setTeamwork(70);        // 0.3 weight

        PerformanceReview review = new PerformanceReview();
        review.setMetrics(metrics);
        review.calculateOverallScore();

        // Expected: (80 * 0.4) + (90 * 0.3) + (70 * 0.3) = 80
        assertEquals(80.0, review.getOverallScore(), 0.01);
    }

    @Test
    void validateMetrics_InvalidRange_ThrowsException() {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setGoalAchievement(101); // Invalid value
        metrics.setSkillLevel(90);
        metrics.setTeamwork(85);

        PerformanceReview review = new PerformanceReview();
        review.setMetrics(metrics);

        assertThrows(IllegalArgumentException.class, review::calculateOverallScore);
    }
}
