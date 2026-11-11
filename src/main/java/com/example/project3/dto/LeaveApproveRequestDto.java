package com.example.project3.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class  LeaveApproveRequestDto {

    @NotNull(message = "승인자 ID는 필수입니다.")
    @Positive(message = "승인자 ID는 1 이상이어야 합니다.")
    private Long approverId;
}