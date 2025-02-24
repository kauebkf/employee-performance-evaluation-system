package com.example.integration;

import com.example.dto.*;
import com.example.model.EmployeeInfo;
import com.example.model.PerformanceMetrics;
import com.example.repository.PerformanceReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.data.mongodb.database=test",
    "spring.mongodb.embedded.version=4.0.2"
})
public class PerformanceReviewIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PerformanceReviewRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void submitAndRetrievePerformanceReview() {
        // Given
        PerformanceMetrics metrics = new PerformanceMetrics(85.0, 90.0, 95.0);
        EmployeeInfo employeeInfo = new EmployeeInfo();
        employeeInfo.setDepartmentId("dev_dept");
        employeeInfo.setRole("developer");
        
        PerformanceReviewRequest request = new PerformanceReviewRequest();
        request.setEmployeeId("emp1");
        request.setReviewerId("reviewer1");
        request.setMetrics(metrics);
        request.setEmployeeInfo(employeeInfo);
        request.setComments("Great work!");

        // When - Submit review
        ResponseEntity<SubmissionResponse> submitResponse = 
            restTemplate.postForEntity("/reviews", request, SubmissionResponse.class);

        // Then
        assertEquals(HttpStatus.OK, submitResponse.getStatusCode());
        assertNotNull(submitResponse.getBody());
        assertNotNull(submitResponse.getBody().getReviewId());

        // When - Get performance report
        ResponseEntity<PerformanceReport> reportResponse = 
            restTemplate.getForEntity("/employees/emp1/performance", PerformanceReport.class);

        // Then
        assertEquals(HttpStatus.OK, reportResponse.getStatusCode());
        PerformanceReport report = reportResponse.getBody();
        assertNotNull(report);
        assertEquals("emp1", report.getEmployeeId());
        assertEquals("dev_dept", report.getDepartmentId());
        assertEquals(89.5, report.getAverageScore(), 0.01); // (85*0.4 + 90*0.3 + 95*0.3)
    }

    @Test
    void testPeerComparison() {
        // Given - Create multiple reviews for different employees
        createAndSubmitReview("emp1", 85.0); // Score: 85
        createAndSubmitReview("emp2", 90.0); // Score: 90
        createAndSubmitReview("emp3", 80.0); // Score: 80

        // When
        ResponseEntity<PeerComparison> response = 
            restTemplate.getForEntity("/employees/emp1/peer-comparison", PeerComparison.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        PeerComparison comparison = response.getBody();
        assertNotNull(comparison);
        assertEquals("emp1", comparison.getEmployeeId());
        assertEquals("dev_dept", comparison.getDepartmentId());
        assertEquals(85.0, comparison.getAverageScore(), 0.01);
        assertEquals(50.0, comparison.getPercentileRank(), 0.01); // Middle performer
        assertEquals(85.0, comparison.getPeerAverageScore(), 0.01); // (90 + 80) / 2
    }

    @Test
    void testDepartmentSummary() {
        // Given - Create reviews for multiple employees
        createAndSubmitReview("emp1", 95.0); // Top performer
        createAndSubmitReview("emp2", 90.0); // Top performer
        createAndSubmitReview("emp3", 80.0); // Low performer
        createAndSubmitReview("emp4", 75.0); // Low performer

        // When
        ResponseEntity<DepartmentSummary> response = 
            restTemplate.getForEntity("/departments/dev_dept/performance-summary", DepartmentSummary.class);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DepartmentSummary summary = response.getBody();
        assertNotNull(summary);
        assertEquals("dev_dept", summary.getDepartmentId());
        assertEquals(85.0, summary.getAverageScore(), 0.01); // (95 + 90 + 80 + 75) / 4

        // Verify top performers
        List<DepartmentSummary.EmployeePerformance> topPerformers = summary.getTopPerformers();
        assertEquals(2, topPerformers.size());
        assertEquals("emp1", topPerformers.get(0).getEmployeeId());
        assertEquals(95.0, topPerformers.get(0).getOverallScore(), 0.01);
        assertEquals(Integer.valueOf(1), topPerformers.get(0).getRank());
        assertEquals("emp2", topPerformers.get(1).getEmployeeId());
        assertEquals(90.0, topPerformers.get(1).getOverallScore(), 0.01);
        assertEquals(Integer.valueOf(2), topPerformers.get(1).getRank());

        // Verify low performers
        List<DepartmentSummary.EmployeePerformance> lowPerformers = summary.getLowPerformers();
        assertEquals(2, lowPerformers.size());
        assertEquals("emp3", lowPerformers.get(0).getEmployeeId());
        assertEquals(80.0, lowPerformers.get(0).getOverallScore(), 0.01);
        assertNull(lowPerformers.get(0).getRank());
        assertEquals("emp4", lowPerformers.get(1).getEmployeeId());
        assertEquals(75.0, lowPerformers.get(1).getOverallScore(), 0.01);
        assertNull(lowPerformers.get(1).getRank());
    }

    private void createAndSubmitReview(String employeeId, double score) {
        PerformanceMetrics metrics = new PerformanceMetrics(score, score, score);
        EmployeeInfo employeeInfo = new EmployeeInfo();
        employeeInfo.setDepartmentId("dev_dept");
        employeeInfo.setRole("developer");
        
        PerformanceReviewRequest request = new PerformanceReviewRequest();
        request.setEmployeeId(employeeId);
        request.setReviewerId("reviewer1");
        request.setMetrics(metrics);
        request.setEmployeeInfo(employeeInfo);
        request.setComments("Test review");

        restTemplate.postForEntity("/reviews", request, SubmissionResponse.class);
    }
}
