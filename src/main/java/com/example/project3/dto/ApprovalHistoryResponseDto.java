package com.example.project3.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor // join -> 직접 매핑
public class ApprovalHistoryResponseDto {

    private Long historyId;

    private Long requestId;

    private Long actorEmployeeId;

    private String actorName;

    private String actorDepartment;

    private String action;

    private String comment;

    private LocalDateTime actedAt;
}