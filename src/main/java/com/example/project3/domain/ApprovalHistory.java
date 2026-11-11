package com.example.project3.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ApprovalHistory {

    private Long historyId;

    private Long requestId;

    private Long actorEmployeeId;

    private String action;

    private String comment;

    private LocalDateTime actedAt;
}