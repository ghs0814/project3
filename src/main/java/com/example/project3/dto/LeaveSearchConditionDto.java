package com.example.project3.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveSearchConditionDto {

    // 신청자 ID
    @Positive(message = "신청자 ID는 1 이상이어야 합니다.")
    private Long employeeId;

    // 승인자 ID
    @Positive(message = "승인자 ID는 1 이상이어야 합니다.")
    private Long approverId;

    // 휴가 신청 상태
    @Pattern(
            regexp = "PENDING|APPROVED|REJECTED|CANCELED",
            message = "올바르지 않은 휴가 상태입니다."
    )
    private String status;

    // 정렬 순서 (기본값: 최신순)
    @Pattern(
            regexp = "newest|oldest",
            message = "sort는 newest 또는 oldest만 사용할 수 있습니다."
    )
    private String sort = "newest";

    // 5. 신규: 현재 페이지 (1부터 시작)
    @NotNull(message = "페이지 번호는 필수입니다.")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private Integer page = 1;

    // 6. 신규: 페이지 크기
    @NotNull(message = "페이지 크기는 필수입니다.")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private Integer size = 10;

    // 7. 신규: OFFSET 계산
    public long getOffset() {
        return ((long) page - 1) * size;
    }
}