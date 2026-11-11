package com.example.project3.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


// 1. requestBody로 받는 dto는 jackson이 json을 객체로 변환할 수 있도록 setter + noargsconstructor
// 2. mybatis mapper에서 직접적으로 연결되는 dto는 mybatis가 객체를 생성할 수 있도록 setter + noargsconstructor
// 3. responseDto처럼, 개발자가 직접 객체를 생성하고 응답하기 위해 사용하는 dto는 getter + allargsconsturctor

@Getter
@Setter
@NoArgsConstructor
public class LeaveRejectRequestDto {

    @NotNull(message = "승인자 ID는 필수입니다.")
    @Positive(message = "승인자 ID는 1 이상이어야 합니다.")
    private Long approverId;

    @NotBlank(message = "반려 사유는 필수입니다.")
    @Size(max = 500, message = "반려 사유는 500자 이하여야 합니다.")
    private String comment;


}
