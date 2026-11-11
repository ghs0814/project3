package com.example.project3.dto;

import com.example.project3.domain.LeaveRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LeaveResponseDto {

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

    public static LeaveResponseDto from(LeaveRequest leaveRequest) {

        return new LeaveResponseDto(
                leaveRequest.getRequestId(),
                leaveRequest.getEmployeeId(),
                leaveRequest.getApproverId(),
                leaveRequest.getStartDate(),
                leaveRequest.getEndDate(),
                leaveRequest.getReason(),
                leaveRequest.getStatus(),
                leaveRequest.getCreatedAt(),
                leaveRequest.getDecidedAt(),
                leaveRequest.getRejectionComment()
        );
    }
}