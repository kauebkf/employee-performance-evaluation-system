package com.example.repository;

import com.example.model.PerformanceReview;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PerformanceReviewRepository extends MongoRepository<PerformanceReview, String> {

    List<PerformanceReview> findByEmployeeId(String employeeId);

    @Query("{ 'employeeId': ?0, 'reviewDate': { $gte: ?1, $lte: ?2 } }")
    List<PerformanceReview> findByEmployeeIdAndReviewDateBetween(
            String employeeId, LocalDate startDate, LocalDate endDate);

    @Aggregation(pipeline = {
        "{ $match: { 'employeeInfo.departmentId': ?0, 'employeeInfo.role': ?1 } }",
        "{ $group: { _id: '$employeeId', avgScore: { $avg: '$overallScore' } } }",
        "{ $project: { _id: 0, id: '$_id', avgScore: 1 } }"
    })
    List<AggregationResult> getPeerAggregation(String departmentId, String role);

    @Aggregation(pipeline = {
        "{ $match: { 'employeeInfo.departmentId': ?0 } }",
        "{ $sort: { 'reviewDate': -1 } }",
        "{ $group: { _id: '$employeeId', avgScore: { $avg: '$overallScore' }, latestRole: { $first: '$employeeInfo.role' } } }",
        "{ $project: { _id: 0, id: '$_id', avgScore: 1, latestRole: 1 } }"
    })
    List<DepartmentResult> getDepartmentAggregation(String departmentId);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class AggregationResult {
        private String id;
        private double avgScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class DepartmentResult {
        private String id;
        private double avgScore;
        private String latestRole;
    }
}
