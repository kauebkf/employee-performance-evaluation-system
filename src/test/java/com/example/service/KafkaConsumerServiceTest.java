package com.example.service;

import com.example.dto.PerformanceReviewRequest;
import com.example.dto.SubmissionResponse;
import com.example.model.EmployeeInfo;
import com.example.model.PerformanceMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkaConsumerServiceTest {

    @Mock
    private PerformanceReviewService performanceReviewService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    private static final String TOPIC = "performance-reviews";
    private static final int PARTITION = 0;
    private static final long OFFSET = 123L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void consumePerformanceReview_ValidMessage_ProcessesSuccessfully() throws Exception {
        // Given
        String message = "{\"employeeId\":\"emp1\",\"reviewerId\":\"reviewer1\"}";
        PerformanceReviewRequest request = createValidRequest();
        SubmissionResponse response = new SubmissionResponse("review1", "submitted");

        when(objectMapper.readValue(message, PerformanceReviewRequest.class)).thenReturn(request);
        when(performanceReviewService.submitReview(request)).thenReturn(response);

        // When
        kafkaConsumerService.consumePerformanceReview(message, TOPIC, PARTITION, OFFSET);

        // Then
        verify(objectMapper).readValue(message, PerformanceReviewRequest.class);
        verify(performanceReviewService).submitReview(request);
    }

    @Test
    void consumePerformanceReview_InvalidJson_ThrowsException() throws Exception {
        // Given
        String invalidMessage = "{invalid-json}";
        when(objectMapper.readValue(invalidMessage, PerformanceReviewRequest.class))
            .thenThrow(new RuntimeException("Invalid JSON"));

        // When/Then
        assertThrows(RuntimeException.class, () -> 
            kafkaConsumerService.consumePerformanceReview(invalidMessage, TOPIC, PARTITION, OFFSET));
        verify(performanceReviewService, never()).submitReview(any());
    }

    @Test
    void consumePerformanceReview_ServiceError_ThrowsException() throws Exception {
        // Given
        String message = "{\"employeeId\":\"emp1\",\"reviewerId\":\"reviewer1\"}";
        PerformanceReviewRequest request = createValidRequest();

        when(objectMapper.readValue(message, PerformanceReviewRequest.class)).thenReturn(request);
        when(performanceReviewService.submitReview(request))
            .thenThrow(new RuntimeException("Service error"));

        // When/Then
        assertThrows(RuntimeException.class, () -> 
            kafkaConsumerService.consumePerformanceReview(message, TOPIC, PARTITION, OFFSET));
    }

    private PerformanceReviewRequest createValidRequest() {
        PerformanceReviewRequest request = new PerformanceReviewRequest();
        request.setEmployeeId("emp1");
        request.setReviewerId("reviewer1");
        request.setReviewDate(LocalDate.now().toString());
        
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setGoalAchievement(85);
        metrics.setSkillLevel(90);
        metrics.setTeamwork(95);
        request.setMetrics(metrics);

        EmployeeInfo employeeInfo = new EmployeeInfo();
        employeeInfo.setDepartmentId("dev_dept");
        employeeInfo.setRole("developer");
        request.setEmployeeInfo(employeeInfo);

        return request;
    }
}
