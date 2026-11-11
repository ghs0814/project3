package com.example.project3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class LeaveCreateRequestDto {

    @NotNull(message = "신청 직원 ID는 필수입니다.")
    @Positive(message = "신청 직원 ID는 1 이상이어야 합니다.")
    private Long employeeId;

    @NotNull(message = "승인 직원 ID는 필수입니다.")
    @Positive(message = "승인 직원 ID는 1 이상이어야 합니다.")
    private Long approverId;

    @NotNull(message = "휴가 시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "휴가 종료일은 필수입니다.")
    private LocalDate endDate;

    @NotBlank(message = "휴가 신청 사유는 필수입니다.")
    @Size(max = 500, message = "휴가 신청 사유는 500자 이하여야 합니다.")
    private String reason;
}