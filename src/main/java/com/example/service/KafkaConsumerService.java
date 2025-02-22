package com.example.service;

import com.example.dto.PerformanceReviewRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final PerformanceReviewService performanceReviewService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topic.performance-reviews}",
        groupId = "performance-review-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePerformanceReview(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received message from Kafka - Topic: {}, Partition: {}, Offset: {}", 
                topic, partition, offset);
        
        try {
            PerformanceReviewRequest review = objectMapper.readValue(message, PerformanceReviewRequest.class);
            performanceReviewService.submitReview(review);
            log.info("Successfully processed performance review from Kafka");
        } catch (Exception e) {
            log.error("Error processing performance review from Kafka: {}", e.getMessage(), e);
            // The message will be retried by Kafka since we have disabled auto-commit
            throw new RuntimeException("Error processing performance review", e);
        }
    }
}
