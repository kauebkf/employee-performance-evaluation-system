package com.example.repository;

import com.example.model.PerformanceReview;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import java.time.LocalDate;
import java.util.List;

public interface PerformanceReviewRepository extends MongoRepository<PerformanceReview, String> {
    List<PerformanceReview> findByEmployeeId(String employeeId);
    
    List<PerformanceReview> findByEmployeeInfo_DepartmentId(String departmentId);
    
    @Query("{'employeeId': ?0, 'reviewDate': {'$gte': ?1, '$lte': ?2}}")
    List<PerformanceReview> findByEmployeeIdAndReviewDateBetween(
        String employeeId, LocalDate startDate, LocalDate endDate);
    
    @Aggregation(pipeline = {
        "{ $match: { 'employeeInfo.departmentId': ?0 } }",
        "{ $group: { " +
        "    _id: '$employeeId', " +
        "    avgScore: { $avg: '$overallScore' }, " +
        "    latestRole: { $last: '$employeeInfo.role' } " +
        "} }"
    })
    List<DepartmentAggregationResult> getDepartmentAggregation(String departmentId);

    @Aggregation(pipeline = {
        "{ $match: { 'employeeInfo.departmentId': ?0, 'employeeInfo.role': ?1 } }",
        "{ $group: { " +
        "    _id: '$employeeId', " +
        "    avgScore: { $avg: '$overallScore' } " +
        "} }"
    })
    List<PeerAggregationResult> getPeerAggregation(String departmentId, String role);

    interface DepartmentAggregationResult {
        String getId();
        double getAvgScore();
        String getLatestRole();
    }

    interface PeerAggregationResult {
        String getId();
        double getAvgScore();
    }
}
