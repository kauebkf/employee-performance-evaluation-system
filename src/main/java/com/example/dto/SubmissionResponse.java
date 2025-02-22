package com.example.dto;

import lombok.Data;

@Data
public class SubmissionResponse {
    private String reviewId;
    private String status;

    public static SubmissionResponse of(String reviewId) {
        SubmissionResponse response = new SubmissionResponse();
        response.setReviewId(reviewId);
        response.setStatus("submitted");
        return response;
    }
}
