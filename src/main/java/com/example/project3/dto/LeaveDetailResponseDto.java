package com.example.project3.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor // mybatis가 직접 객체를 생성하고, set하기 위해
public class LeaveDetailResponseDto {

    private Long requestId;

    private Long employeeId;

    private String applicantName;

    private String applicantDepartment;

    private Long approverId;

    private String approverName;

    private String approverDepartment;

    private LocalDate startDate;

    private LocalDate endDate;

    private String reason;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    private String rejectionComment;
}