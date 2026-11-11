package com.example.project3.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest {

    private Long requestId;

    private Long employeeId;

    private Long approverId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String reason;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    private String rejectionComment;
}